package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.application.dto.Comandos;
import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.atendimento.model.ResultadoDeAvaliacao;
import school.cesar.gymflow.domain.atendimento.model.SolicitacaoDeAvaliacao;
import school.cesar.gymflow.domain.atendimento.model.StatusAvaliacao;
import school.cesar.gymflow.domain.atendimento.repositorio.RepositorioDeSolicitacoes;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Funcionalidade: agendamento e realizacao da avaliacao fisica.
 * Regras entre agregados (aqui no caso de uso):
 * - exige matricula ativa e apenas uma avaliacao em aberto por aluno;
 * - agenda unica: um avaliador, um aluno por horario (confirmado ou proposto);
 * - intervalo minimo de 30 dias apos a ultima avaliacao realizada;
 * - 2 faltas nos ultimos 90 dias bloqueiam novos agendamentos por 30 dias desde a ultima falta.
 */
public class AvaliacaoFisicaUseCase {

    public static final int DIAS_ENTRE_AVALIACOES = 30;
    public static final int FALTAS_PARA_BLOQUEIO = 2;
    public static final int JANELA_DE_FALTAS_DIAS = 90;
    public static final int DIAS_DE_BLOQUEIO = 30;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RepositorioDeSolicitacoes solicitacoes;
    private final RepositorioDeMatriculas matriculas;
    private final NomesDeAlunos nomes;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public AvaliacaoFisicaUseCase(RepositorioDeSolicitacoes solicitacoes, RepositorioDeMatriculas matriculas,
                                  NomesDeAlunos nomes, PublicadorDeEventos publicador, Relogio relogio) {
        this.solicitacoes = solicitacoes;
        this.matriculas = matriculas;
        this.nomes = nomes;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transacional
    public Respostas.AvaliacaoResumo solicitar(Long alunoId, Comandos.SolicitarAvaliacao comando) {
        LocalDateTime agora = relogio.agora();
        if (matriculas.vigentePorAluno(alunoId).isEmpty()) {
            throw new RegraDeNegocioException("Avaliacao fisica exige matricula ativa");
        }
        List<SolicitacaoDeAvaliacao> doAluno = solicitacoes.avaliacoesDoAluno(alunoId);
        if (doAluno.stream().anyMatch(a -> a.emAberto(agora))) {
            throw new RegraDeNegocioException("Voce ja tem uma avaliacao fisica em aberto");
        }
        List<SolicitacaoDeAvaliacao> faltas = doAluno.stream()
                .filter(a -> a.status() == StatusAvaliacao.NAO_COMPARECEU
                        && a.dataAgendada().isAfter(agora.minusDays(JANELA_DE_FALTAS_DIAS)))
                .sorted(Comparator.comparing(SolicitacaoDeAvaliacao::dataAgendada).reversed()).toList();
        if (faltas.size() >= FALTAS_PARA_BLOQUEIO
                && agora.isBefore(faltas.get(0).dataAgendada().plusDays(DIAS_DE_BLOQUEIO))) {
            throw new RegraDeNegocioException("Agendamento de avaliacao bloqueado por " + FALTAS_PARA_BLOQUEIO
                    + " faltas nos ultimos " + JANELA_DE_FALTAS_DIAS + " dias");
        }
        doAluno.stream().filter(a -> a.status() == StatusAvaliacao.REALIZADA)
                .map(SolicitacaoDeAvaliacao::realizadaEm).max(Comparator.naturalOrder())
                .ifPresent(ultima -> {
                    if (comando.dataHora() != null && comando.dataHora().isBefore(ultima.plusDays(DIAS_ENTRE_AVALIACOES))) {
                        throw new RegraDeNegocioException("Nova avaliacao so pode ser marcada " + DIAS_ENTRE_AVALIACOES
                                + " dias apos a ultima realizada");
                    }
                });
        return salvar(SolicitacaoDeAvaliacao.solicitar(alunoId, comando.dataHora(), agora), "solicitada");
    }

    @Transacional
    public Respostas.AvaliacaoResumo confirmar(Long id, String observacao) {
        SolicitacaoDeAvaliacao s = buscar(id);
        exigirHorarioLivre(s.dataDesejada(), s.id());
        s.confirmar(observacao, relogio.agora());
        return salvar(s, "confirmada para " + s.dataAgendada().format(BR));
    }

    @Transacional
    public Respostas.AvaliacaoResumo proporHorario(Long id, Comandos.ResponderAvaliacao comando) {
        SolicitacaoDeAvaliacao s = buscar(id);
        if (comando.novaDataHora() != null) {
            exigirHorarioLivre(comando.novaDataHora(), s.id());
        }
        s.proporNovoHorario(comando.novaDataHora(), comando.observacao(), relogio.agora());
        return salvar(s, "novo horario proposto: " + s.dataProposta().format(BR));
    }

    @Transacional
    public Respostas.AvaliacaoResumo aceitarProposta(Long alunoId, Long id) {
        SolicitacaoDeAvaliacao s = doAluno(alunoId, id);
        s.aceitarProposta(relogio.agora());
        return salvar(s, "proposta aceita pelo aluno");
    }

    @Transacional
    public Respostas.AvaliacaoResumo recusarProposta(Long alunoId, Long id) {
        SolicitacaoDeAvaliacao s = doAluno(alunoId, id);
        s.recusarProposta();
        return salvar(s, "proposta recusada pelo aluno");
    }

    @Transacional
    public Respostas.AvaliacaoResumo cancelar(Long alunoId, Long id) {
        SolicitacaoDeAvaliacao s = doAluno(alunoId, id);
        s.cancelar(relogio.agora());
        return salvar(s, "cancelada pelo aluno");
    }

    @Transacional
    public Respostas.AvaliacaoResumo registrarResultado(Long id, Comandos.RegistrarResultadoDeAvaliacao comando) {
        SolicitacaoDeAvaliacao s = buscar(id);
        if (comando.pesoKg() == null || comando.alturaM() == null) {
            throw new RegraDeNegocioException("Informe peso e altura");
        }
        ResultadoDeAvaliacao medidas = new ResultadoDeAvaliacao(comando.pesoKg(), comando.alturaM(),
                comando.percentualDeGordura());
        s.registrarResultado(medidas, relogio.agora());
        return salvar(s, "realizada: IMC " + medidas.imc() + " (" + medidas.classificacao() + ")");
    }

    @Transacional
    public Respostas.AvaliacaoResumo registrarFalta(Long id) {
        SolicitacaoDeAvaliacao s = buscar(id);
        s.registrarFalta(relogio.agora());
        return salvar(s, "aluno nao compareceu");
    }

    @Transacional(somenteLeitura = true)
    public List<Respostas.AvaliacaoResumo> doAluno(Long alunoId) {
        String nome = nomes.de(alunoId);
        return solicitacoes.avaliacoesDoAluno(alunoId).stream().map(a -> resumo(a, nome)).toList();
    }

    @Transacional(somenteLeitura = true)
    public List<Respostas.AvaliacaoResumo> todas() {
        Map<Long, String> mapa = nomes.mapa();
        return solicitacoes.avaliacoes().stream().map(a -> resumo(a, mapa.getOrDefault(a.alunoId(), "-"))).toList();
    }

    private void exigirHorarioLivre(LocalDateTime horario, Long ignorar) {
        boolean ocupado = solicitacoes.avaliacoes().stream()
                .anyMatch(a -> !a.id().equals(ignorar) && horario.equals(a.horarioOcupado()));
        if (ocupado) {
            throw new RegraDeNegocioException("Horario indisponivel: ja existe avaliacao marcada para " + horario.format(BR));
        }
    }

    private SolicitacaoDeAvaliacao buscar(Long id) {
        return solicitacoes.avaliacaoPorId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Solicitacao de avaliacao nao encontrada"));
    }

    private SolicitacaoDeAvaliacao doAluno(Long alunoId, Long id) {
        SolicitacaoDeAvaliacao s = buscar(id);
        if (!s.alunoId().equals(alunoId)) {
            throw new RegraDeNegocioException("Solicitacao de avaliacao nao encontrada");
        }
        return s;
    }

    private Respostas.AvaliacaoResumo salvar(SolicitacaoDeAvaliacao s, String detalhe) {
        SolicitacaoDeAvaliacao salva = solicitacoes.salvar(s);
        String nome = nomes.de(salva.alunoId());
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.AVALIACAO_FISICA, String.valueOf(salva.id()),
                "Avaliacao fisica de " + nome + " " + detalhe));
        return resumo(salva, nome);
    }

    Respostas.AvaliacaoResumo resumo(SolicitacaoDeAvaliacao a, String nome) {
        ResultadoDeAvaliacao r = a.resultado();
        return new Respostas.AvaliacaoResumo(a.id(), a.alunoId(), nome, a.dataDesejada(), a.dataProposta(),
                a.dataAgendada(), a.status().name(), a.observacao(), a.emAberto(relogio.agora()), a.criadaEm(),
                SolicitacaoDeAvaliacao.MAX_PROPOSTAS - a.propostasFeitas(),
                r == null ? null : r.pesoKg(), r == null ? null : r.alturaM(), r == null ? null : r.percentualDeGordura(),
                r == null ? null : r.imc(), r == null ? null : r.classificacao(), a.realizadaEm());
    }
}
