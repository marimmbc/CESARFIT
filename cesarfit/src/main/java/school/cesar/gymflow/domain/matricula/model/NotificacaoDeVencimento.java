package school.cesar.gymflow.domain.matricula.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/**
 * Aviso enviado ao aluno pela regua de vencimento (lembrete, vence em breve, vence hoje,
 * vencida e ultimo aviso). Uma notificacao por matricula, vencimento e etapa.
 */
public class NotificacaoDeVencimento {

    private Long id;
    private final Long matriculaId;
    private final Long alunoId;
    private final LocalDate vencimento;
    private final EtapaDeAviso etapa;
    private final LocalDateTime enviadaEm;
    private final String mensagem;
    private boolean lida;

    public NotificacaoDeVencimento(Long id, Long matriculaId, Long alunoId, LocalDate vencimento, EtapaDeAviso etapa,
                                   LocalDateTime enviadaEm, String mensagem, boolean lida) {
        this.id = id;
        this.matriculaId = matriculaId;
        this.alunoId = alunoId;
        this.vencimento = vencimento;
        this.etapa = etapa == null ? EtapaDeAviso.PROXIMO_DO_VENCIMENTO : etapa;
        this.enviadaEm = enviadaEm;
        this.mensagem = mensagem;
        this.lida = lida;
    }

    /**
     * Decide qual etapa da regua deve ser enviada hoje para a matricula (ou null):
     * - matricula cancelada nunca recebe aviso;
     * - avisos de cobranca (apos o vencimento) so se o aluno nao tiver outra matricula vigente;
     * - o lembrete de 7 dias so vale para planos com vigencia de pelo menos 15 dias;
     * - nenhuma etapa e repetida e nenhuma etapa anterior a uma ja enviada e enviada (sem regressao);
     * - se a verificacao atrasar, envia apenas a etapa mais urgente do dia (nao "despeja" as perdidas).
     */
    public static EtapaDeAviso etapaDevida(Matricula matricula, LocalDate hoje, Set<EtapaDeAviso> jaEnviadas,
                                           boolean alunoTemOutraMatriculaVigente) {
        if (matricula.status() == StatusMatricula.CANCELADA) {
            return null;
        }
        EtapaDeAviso etapa = EtapaDeAviso.para(ChronoUnit.DAYS.between(hoje, matricula.fim()));
        if (etapa == null || jaEnviadas.contains(etapa)) {
            return null;
        }
        boolean jaPassou = jaEnviadas.stream().anyMatch(e -> e.ordinal() > etapa.ordinal());
        if (jaPassou) {
            return null;
        }
        if (etapa.cobranca() && alunoTemOutraMatriculaVigente) {
            return null;
        }
        if (etapa == EtapaDeAviso.LEMBRETE
                && ChronoUnit.DAYS.between(matricula.inicio(), matricula.fim()) < EtapaDeAviso.VIGENCIA_MINIMA_PARA_LEMBRETE) {
            return null;
        }
        return etapa;
    }

    public static NotificacaoDeVencimento para(Matricula matricula, EtapaDeAviso etapa, String nomeDoAluno,
                                               LocalDate hoje, LocalDateTime agora) {
        if (etapa == null) {
            throw new RegraDeNegocioException("Matricula fora da regua de aviso de vencimento");
        }
        long dias = ChronoUnit.DAYS.between(hoje, matricula.fim());
        String plano = matricula.descricaoDoPlano();
        String mensagem = switch (etapa) {
            case LEMBRETE -> "Ola, " + nomeDoAluno + "! Sua matricula no " + plano + " vence em " + dias
                    + " dias. Renove com antecedencia e nao perca nenhum treino.";
            case PROXIMO_DO_VENCIMENTO -> "Ola, " + nomeDoAluno + "! Sua matricula no " + plano + " vence "
                    + (dias == 1 ? "amanha" : "em " + dias + " dias") + ". Procure a recepcao para renovar.";
            case VENCE_HOJE -> "Ola, " + nomeDoAluno + "! Sua matricula no " + plano
                    + " vence hoje. Renove para continuar treinando amanha.";
            case VENCIDA -> "Ola, " + nomeDoAluno + ". Sua matricula no " + plano + " venceu ha " + (-dias)
                    + " dia(s). Regularize o pagamento na recepcao.";
            case ULTIMO_AVISO -> "Ola, " + nomeDoAluno + ". Ultimo aviso: sua matricula no " + plano
                    + " esta vencida ha " + (-dias) + " dias. Renove para reativar seu acesso e sua ficha.";
        };
        return new NotificacaoDeVencimento(null, matricula.id(), matricula.alunoId(), matricula.fim(), etapa,
                agora, mensagem, false);
    }

    public void marcarComoLida() {
        if (lida) {
            throw new RegraDeNegocioException("Aviso ja foi marcado como lido");
        }
        lida = true;
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public Long matriculaId() { return matriculaId; }
    public Long alunoId() { return alunoId; }
    public LocalDate vencimento() { return vencimento; }
    public EtapaDeAviso etapa() { return etapa; }
    public LocalDateTime enviadaEm() { return enviadaEm; }
    public String mensagem() { return mensagem; }
    public boolean lida() { return lida; }
}
