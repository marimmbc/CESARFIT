package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.operacao.model.OcorrenciaDeEquipamento;
import school.cesar.gymflow.domain.operacao.model.Severidade;
import school.cesar.gymflow.domain.operacao.repositorio.RepositorioDeOcorrencias;
import school.cesar.gymflow.domain.shared.Dinheiro;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;
import school.cesar.gymflow.domain.shared.evento.BarramentoDeEventos;

/**
 * Funcionalidade: gestao de ocorrencias de equipamentos com severidade, SLA, pausa por peca,
 * reabertura, reincidencia e interdicao do equipamento.
 */
@Service
public class OcorrenciaDeEquipamentoUseCase {

    private final RepositorioDeOcorrencias ocorrencias;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;
    private final BarramentoDeEventos barramento;

    public OcorrenciaDeEquipamentoUseCase(RepositorioDeOcorrencias ocorrencias, PublicadorDeEventos publicador,
                                          Relogio relogio, BarramentoDeEventos barramento) {
        this.barramento = barramento;
        this.ocorrencias = ocorrencias;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Respostas.OcorrenciaResumo registrar(Comandos.RegistrarOcorrencia comando) {
        LocalDateTime agora = relogio.agora();
        List<OcorrenciaDeEquipamento> doEquipamento = ocorrencias.todas().stream()
                .filter(o -> o.doEquipamento(comando.equipamento())).toList();
        if (doEquipamento.stream().anyMatch(OcorrenciaDeEquipamento::pendente)) {
            throw new RegraDeNegocioException("Ja existe uma ocorrencia pendente para o equipamento "
                    + (comando.equipamento() == null ? "" : comando.equipamento().trim()));
        }
        LocalDateTime janela = agora.minusDays(OcorrenciaDeEquipamento.JANELA_DE_REINCIDENCIA_DIAS);
        long resolvidasRecentes = doEquipamento.stream().filter(o -> o.resolvidaDesde(janela)).count();
        OcorrenciaDeEquipamento nova = OcorrenciaDeEquipamento.registrar(comando.equipamento(), comando.descricao(),
                Severidade.de(comando.severidade()), resolvidasRecentes, agora);
        String detalhe = "registrada (" + nova.severidade() + ")" + (nova.reincidente() ? " REINCIDENTE" : "")
                + (nova.interditado() ? " - equipamento INTERDITADO" : "") + ": " + nova.descricao();
        return salvar(nova, detalhe);
    }

    @Transactional
    public Respostas.OcorrenciaResumo iniciar(Long id) {
        OcorrenciaDeEquipamento o = buscar(id);
        o.iniciarAtendimento(relogio.agora());
        return salvar(o, "em andamento");
    }

    @Transactional
    public Respostas.OcorrenciaResumo aguardarPeca(Long id, String peca) {
        OcorrenciaDeEquipamento o = buscar(id);
        o.aguardarPeca(peca, relogio.agora());
        return salvar(o, "aguardando peca: " + o.pecaAguardada() + " (SLA pausado)");
    }

    @Transactional
    public Respostas.OcorrenciaResumo retomar(Long id) {
        OcorrenciaDeEquipamento o = buscar(id);
        o.retomar(relogio.agora());
        return salvar(o, "retomada (peca recebida)");
    }

    @Transactional
    public Respostas.OcorrenciaResumo resolver(Long id, String solucao) {
        return resolver(id, new Comandos.ResolverOcorrencia(solucao, null));
    }

    @Transactional
    public Respostas.OcorrenciaResumo resolver(Long id, Comandos.ResolverOcorrencia comando) {
        OcorrenciaDeEquipamento o = buscar(id);
        Dinheiro custo = comando.custo() == null || comando.custo().isBlank() ? null : Dinheiro.de(comando.custo());
        boolean noPrazo = !o.atrasada(relogio.agora());
        o.resolver(comando.solucao(), custo, relogio.agora());
        return salvar(o, "resolvida " + (noPrazo ? "dentro" : "FORA") + " do SLA: " + o.solucao());
    }

    @Transactional
    public Respostas.OcorrenciaResumo reabrir(Long id, String motivo) {
        OcorrenciaDeEquipamento o = buscar(id);
        o.reabrir(motivo, relogio.agora());
        return salvar(o, "reaberta (" + o.reaberturas() + "a vez): " + motivo);
    }

    /** Fila de trabalho: atrasadas primeiro, depois por severidade e pelo prazo mais proximo. */
    @Transactional(readOnly = true)
    public List<Respostas.OcorrenciaResumo> listar() {
        LocalDateTime agora = relogio.agora();
        return ocorrencias.todas().stream()
                .sorted(Comparator.comparing((OcorrenciaDeEquipamento o) -> !o.pendente())
                        .thenComparing(o -> !o.atrasada(agora))
                        .thenComparing(o -> -o.severidade().ordinal())
                        .thenComparing(o -> o.prazoDoSla(agora)))
                .map(o -> resumo(o, agora)).toList();
    }

    @Transactional(readOnly = true)
    public Respostas.OcorrenciaResumo detalhar(Long id) {
        return resumo(buscar(id), relogio.agora());
    }

    private OcorrenciaDeEquipamento buscar(Long id) {
        return ocorrencias.porId(id).orElseThrow(() -> new RegraDeNegocioException("Ocorrencia nao encontrada"));
    }

    private Respostas.OcorrenciaResumo salvar(OcorrenciaDeEquipamento o, String detalhe) {
        var eventos = o.retirarEventos();
        OcorrenciaDeEquipamento salva = ocorrencias.salvar(o);
        barramento.postarTodos(eventos);
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.OCORRENCIA_EQUIPAMENTO,
                String.valueOf(salva.id()), salva.equipamento() + " " + detalhe));
        return resumo(salva, relogio.agora());
    }

    static Respostas.OcorrenciaResumo resumo(OcorrenciaDeEquipamento o, LocalDateTime agora) {
        return new Respostas.OcorrenciaResumo(o.id(), o.equipamento(), o.descricao(), o.status().name(),
                o.abertaEm(), o.atualizadaEm(), o.solucao(), o.severidade().name(), o.prazoDoSla(agora),
                o.atrasada(agora), o.interditado(), o.pecaAguardada(), o.minutosPausados(),
                o.custoDoReparo() == null ? null : o.custoDoReparo().toString(), o.reaberturas(), o.reincidente());
    }
}
