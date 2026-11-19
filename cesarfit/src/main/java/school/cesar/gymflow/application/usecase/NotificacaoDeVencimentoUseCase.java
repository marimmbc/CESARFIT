package school.cesar.gymflow.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.matricula.model.EtapaDeAviso;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.matricula.model.NotificacaoDeVencimento;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeNotificacoes;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Funcionalidade: regua automatica de avisos de vencimento (D-7, D-3..D-1, D0, D+1..D+4, D+5..D+15).
 * Roda todo dia (agendador) ou sob demanda. Regras em {@link NotificacaoDeVencimento#etapaDevida}.
 * Alem disso: no maximo um aviso por aluno por verificacao, e a matricula passa a VENCIDA
 * quando entra na fase de cobranca.
 */
public class NotificacaoDeVencimentoUseCase {

    private final RepositorioDeMatriculas matriculas;
    private final RepositorioDeNotificacoes notificacoes;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public NotificacaoDeVencimentoUseCase(RepositorioDeMatriculas matriculas, RepositorioDeNotificacoes notificacoes,
                                          NomesDeAlunos nomes, PublicadorDeEventos publicador, Relogio relogio) {
        this.matriculas = matriculas;
        this.notificacoes = notificacoes;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transacional
    public Respostas.VerificacaoDeVencimentos verificar(LocalDate hoje) {
        Map<Long, String> mapa = nomes.mapa();
        List<Respostas.NotificacaoResumo> enviados = new ArrayList<>();
        Set<Long> alunosAvisados = new HashSet<>();
        for (Matricula matricula : matriculas.todas()) {
            if (alunosAvisados.contains(matricula.alunoId())) {
                continue;
            }
            boolean outraVigente = matriculas.porAluno(matricula.alunoId()).stream()
                    .anyMatch(m -> !m.id().equals(matricula.id()) && m.estaVigenteEm(hoje));
            EtapaDeAviso etapa = NotificacaoDeVencimento.etapaDevida(matricula, hoje,
                    notificacoes.etapasEnviadas(matricula.id(), matricula.fim()), outraVigente);
            if (etapa == null) {
                continue;
            }
            String nome = mapa.getOrDefault(matricula.alunoId(), "aluno");
            NotificacaoDeVencimento salva = notificacoes.salvar(
                    NotificacaoDeVencimento.para(matricula, etapa, nome, hoje, relogio.agora()));
            if (etapa.cobranca()) {
                matricula.atualizarVigencia(hoje);
                matriculas.salvar(matricula);
            }
            alunosAvisados.add(matricula.alunoId());
            publicador.publicar(EventoDeDominio.de(EventoDeDominio.MATRICULA_A_VENCER,
                    String.valueOf(matricula.id()), "[" + etapa.rotulo() + "] " + salva.mensagem()));
            enviados.add(resumo(salva, nome));
        }
        return new Respostas.VerificacaoDeVencimentos(enviados.size(), enviados);
    }

    @Transacional
    public Respostas.NotificacaoResumo marcarComoLida(Long alunoId, Long notificacaoId) {
        NotificacaoDeVencimento n = notificacoes.porId(notificacaoId)
                .filter(x -> x.alunoId().equals(alunoId))
                .orElseThrow(() -> new RegraDeNegocioException("Aviso nao encontrado"));
        n.marcarComoLida();
        return resumo(notificacoes.salvar(n), nomes.de(alunoId));
    }

    @Transacional(somenteLeitura = true)
    public List<Respostas.NotificacaoResumo> doAluno(Long alunoId) {
        String nome = nomes.de(alunoId);
        return notificacoes.porAluno(alunoId).stream().map(n -> resumo(n, nome)).toList();
    }

    @Transacional(somenteLeitura = true)
    public List<Respostas.NotificacaoResumo> todas() {
        Map<Long, String> mapa = nomes.mapa();
        return notificacoes.todas().stream().map(n -> resumo(n, mapa.getOrDefault(n.alunoId(), "-"))).toList();
    }

    static Respostas.NotificacaoResumo resumo(NotificacaoDeVencimento n, String nome) {
        return new Respostas.NotificacaoResumo(n.id(), n.matriculaId(), n.alunoId(), nome, n.vencimento(),
                n.etapa().name(), n.etapa().rotulo(), n.etapa().cobranca(), n.enviadaEm(), n.mensagem(), n.lida());
    }
}
