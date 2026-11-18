package school.cesar.gymflow.domain.atendimento.model;

import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.shared.Texto;

/**
 * Raiz de agregado: pedido de avaliacao fisica.
 *
 * Fluxo: SOLICITADA -> (equipe propoe horario <-> aluno recusa)* -> CONFIRMADA -> REALIZADA | NAO_COMPARECEU,
 * e o aluno pode cancelar enquanto estiver em aberto.
 * Regras do agregado:
 * - horario: futuro, com pelo menos 24h de antecedencia, ate 60 dias, das 6h as 21h, em :00 ou :30;
 * - a equipe pode propor no maximo 2 horarios alternativos;
 * - cancelamento de avaliacao confirmada so ate 2h antes;
 * - o resultado so pode ser registrado depois do horario agendado e gera IMC e classificacao;
 * - a falta so pode ser registrada 15 minutos apos o horario (tolerancia).
 */
public class SolicitacaoDeAvaliacao {

    public static final int ABERTURA = 6;
    public static final int FECHAMENTO = 21;
    public static final int DIAS_MAXIMOS = 60;
    public static final int HORAS_DE_ANTECEDENCIA = 24;
    public static final int MAX_PROPOSTAS = 2;
    public static final int HORAS_MINIMAS_PARA_CANCELAR = 2;
    public static final int MINUTOS_DE_TOLERANCIA = 15;

    private Long id;
    private final Long alunoId;
    private final LocalDateTime dataDesejada;
    private LocalDateTime dataProposta;
    private LocalDateTime dataAgendada;
    private StatusAvaliacao status;
    private String observacao;
    private final LocalDateTime criadaEm;
    private int propostasFeitas;
    private ResultadoDeAvaliacao resultado;
    private LocalDateTime realizadaEm;

    public SolicitacaoDeAvaliacao(Long id, Long alunoId, LocalDateTime dataDesejada, LocalDateTime dataProposta,
                                  LocalDateTime dataAgendada, StatusAvaliacao status, String observacao,
                                  LocalDateTime criadaEm, int propostasFeitas, ResultadoDeAvaliacao resultado,
                                  LocalDateTime realizadaEm) {
        this.id = id;
        this.alunoId = alunoId;
        this.dataDesejada = dataDesejada;
        this.dataProposta = dataProposta;
        this.dataAgendada = dataAgendada;
        this.status = status;
        this.observacao = observacao;
        this.criadaEm = criadaEm;
        this.propostasFeitas = propostasFeitas;
        this.resultado = resultado;
        this.realizadaEm = realizadaEm;
    }

    public static SolicitacaoDeAvaliacao solicitar(Long alunoId, LocalDateTime desejada, LocalDateTime agora) {
        validarHorario(desejada, agora);
        return new SolicitacaoDeAvaliacao(null, alunoId, desejada, null, null,
                StatusAvaliacao.SOLICITADA, null, agora, 0, null, null);
    }

    static void validarHorario(LocalDateTime data, LocalDateTime agora) {
        if (data == null || !data.isAfter(agora)) {
            throw new RegraDeNegocioException("A data da avaliacao deve ser futura");
        }
        if (data.isBefore(agora.plusHours(HORAS_DE_ANTECEDENCIA))) {
            throw new RegraDeNegocioException("A avaliacao deve ser marcada com pelo menos "
                    + HORAS_DE_ANTECEDENCIA + " horas de antecedencia");
        }
        if (data.isAfter(agora.plusDays(DIAS_MAXIMOS))) {
            throw new RegraDeNegocioException("A avaliacao deve ser marcada em ate " + DIAS_MAXIMOS + " dias");
        }
        if (data.getHour() < ABERTURA || data.getHour() >= FECHAMENTO) {
            throw new RegraDeNegocioException("Horario fora do funcionamento da academia (6h as 21h)");
        }
        if ((data.getMinute() != 0 && data.getMinute() != 30) || data.getSecond() != 0) {
            throw new RegraDeNegocioException("Escolha um horario redondo (ex.: 14:00 ou 14:30)");
        }
    }

    public void confirmar(String obs, LocalDateTime agora) {
        if (status != StatusAvaliacao.SOLICITADA) {
            throw new RegraDeNegocioException("Somente solicitacao pendente pode ser confirmada");
        }
        if (!dataDesejada.isAfter(agora)) {
            throw new RegraDeNegocioException("O horario pedido ja passou; proponha outro horario");
        }
        dataAgendada = dataDesejada;
        observacao = Texto.opcional(obs, 300, "Observacao");
        status = StatusAvaliacao.CONFIRMADA;
    }

    public void proporNovoHorario(LocalDateTime novaData, String obs, LocalDateTime agora) {
        if (status != StatusAvaliacao.SOLICITADA) {
            throw new RegraDeNegocioException("Somente solicitacao pendente pode receber nova proposta");
        }
        if (propostasFeitas >= MAX_PROPOSTAS) {
            throw new RegraDeNegocioException("Limite de " + MAX_PROPOSTAS
                    + " propostas de horario atingido; confirme o horario pedido ou o aluno deve cancelar");
        }
        validarHorario(novaData, agora);
        if (novaData.equals(dataDesejada) || novaData.equals(dataProposta)) {
            throw new RegraDeNegocioException("O novo horario deve ser diferente dos ja tratados");
        }
        dataProposta = novaData;
        observacao = Texto.opcional(obs, 300, "Observacao");
        propostasFeitas++;
        status = StatusAvaliacao.NOVO_HORARIO_PROPOSTO;
    }

    public void aceitarProposta(LocalDateTime agora) {
        if (status != StatusAvaliacao.NOVO_HORARIO_PROPOSTO) {
            throw new RegraDeNegocioException("Nao ha proposta de novo horario para aceitar");
        }
        if (!dataProposta.isAfter(agora)) {
            throw new RegraDeNegocioException("O horario proposto ja passou; faca uma nova solicitacao");
        }
        dataAgendada = dataProposta;
        status = StatusAvaliacao.CONFIRMADA;
    }

    /** O aluno nao aceita o horario proposto: volta para a equipe, que confirma o original ou propoe outro. */
    public void recusarProposta() {
        if (status != StatusAvaliacao.NOVO_HORARIO_PROPOSTO) {
            throw new RegraDeNegocioException("Nao ha proposta de novo horario para recusar");
        }
        status = StatusAvaliacao.SOLICITADA;
    }

    public void cancelar(LocalDateTime agora) {
        if (!emAberto(agora)) {
            throw new RegraDeNegocioException("Esta solicitacao nao pode mais ser cancelada");
        }
        if (status == StatusAvaliacao.CONFIRMADA
                && agora.isAfter(dataAgendada.minusHours(HORAS_MINIMAS_PARA_CANCELAR))) {
            throw new RegraDeNegocioException("Avaliacao confirmada so pode ser cancelada ate "
                    + HORAS_MINIMAS_PARA_CANCELAR + " horas antes");
        }
        status = StatusAvaliacao.CANCELADA;
    }

    public void registrarResultado(ResultadoDeAvaliacao medidas, LocalDateTime agora) {
        if (status != StatusAvaliacao.CONFIRMADA) {
            throw new RegraDeNegocioException("So e possivel registrar o resultado de uma avaliacao confirmada");
        }
        if (agora.isBefore(dataAgendada)) {
            throw new RegraDeNegocioException("O resultado so pode ser registrado depois do horario agendado");
        }
        resultado = medidas;
        realizadaEm = agora;
        status = StatusAvaliacao.REALIZADA;
    }

    public void registrarFalta(LocalDateTime agora) {
        if (status != StatusAvaliacao.CONFIRMADA) {
            throw new RegraDeNegocioException("So e possivel registrar falta em avaliacao confirmada");
        }
        if (agora.isBefore(dataAgendada.plusMinutes(MINUTOS_DE_TOLERANCIA))) {
            throw new RegraDeNegocioException("Aguarde a tolerancia de " + MINUTOS_DE_TOLERANCIA
                    + " minutos apos o horario para registrar a falta");
        }
        status = StatusAvaliacao.NAO_COMPARECEU;
    }

    /** Em aberto = pendente, com proposta ou confirmada para o futuro. */
    public boolean emAberto(LocalDateTime agora) {
        return switch (status) {
            case SOLICITADA, NOVO_HORARIO_PROPOSTO -> true;
            case CONFIRMADA -> dataAgendada != null && dataAgendada.isAfter(agora);
            case REALIZADA, NAO_COMPARECEU, CANCELADA -> false;
        };
    }

    /** Horario que esta avaliacao ocupa na agenda (confirmado ou proposto), ou null. */
    public LocalDateTime horarioOcupado() {
        return switch (status) {
            case CONFIRMADA -> dataAgendada;
            case NOVO_HORARIO_PROPOSTO -> dataProposta;
            default -> null;
        };
    }

    public void atribuirId(Long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public Long alunoId() { return alunoId; }
    public LocalDateTime dataDesejada() { return dataDesejada; }
    public LocalDateTime dataProposta() { return dataProposta; }
    public LocalDateTime dataAgendada() { return dataAgendada; }
    public StatusAvaliacao status() { return status; }
    public String observacao() { return observacao; }
    public LocalDateTime criadaEm() { return criadaEm; }
    public int propostasFeitas() { return propostasFeitas; }
    public ResultadoDeAvaliacao resultado() { return resultado; }
    public LocalDateTime realizadaEm() { return realizadaEm; }
}
