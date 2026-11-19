package school.cesar.gymflow.application.usecase;

import school.cesar.gymflow.application.Transacional;
import school.cesar.gymflow.domain.matricula.model.Matricula;
import school.cesar.gymflow.domain.shared.Relogio;

import java.time.LocalDate;

import school.cesar.gymflow.domain.matricula.model.Aluno;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeAlunos;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeMatriculas;
import school.cesar.gymflow.domain.shared.EventoDeDominio;
import school.cesar.gymflow.domain.shared.PublicadorDeEventos;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.treino.model.Presenca;
import school.cesar.gymflow.domain.treino.repositorio.RepositorioDePresencas;

/** Caso de uso: check-in do dia (uma presenca por dia, exige matricula vigente). */
public class RegistrarPresencaUseCase {

    private final RepositorioDePresencas presencas;
    private final RepositorioDeAlunos alunos;
    private final RepositorioDeMatriculas matriculas;
    private final PublicadorDeEventos publicador;
    private final Relogio relogio;

    public RegistrarPresencaUseCase(RepositorioDePresencas presencas, RepositorioDeAlunos alunos,
                                    RepositorioDeMatriculas matriculas, PublicadorDeEventos publicador,
                                    Relogio relogio) {
        this.relogio = relogio;
        this.presencas = presencas;
        this.alunos = alunos;
        this.matriculas = matriculas;
        this.publicador = publicador;
    }

    public static final int ABERTURA = 6;
    public static final int FECHAMENTO = 22;

    /**
     * Check-in do proprio dia. Regras: academia aberta (6h as 22h), matricula vigente, nao trancada,
     * sem pagamento pendente depois da tolerancia de 5 dias, e um check-in por dia.
     */
    @Transacional
    public void executar(Long alunoId) {
        java.time.LocalDateTime agora = relogio.agora();
        if (agora.getHour() < ABERTURA || agora.getHour() >= FECHAMENTO) {
            throw new RegraDeNegocioException("Check-in permitido apenas das " + ABERTURA + "h as " + FECHAMENTO + "h");
        }
        registrarEm(alunoId, agora.toLocalDate());
    }

    /** Tambem usado pela carga inicial para montar o historico de demonstracao. */
    @Transacional
    public void registrarEm(Long alunoId, LocalDate data) {
        Aluno aluno = alunos.porId(alunoId)
                .orElseThrow(() -> new RegraDeNegocioException("Aluno nao encontrado"));
        if (data.isAfter(relogio.hoje())) {
            throw new RegraDeNegocioException("Data da presenca invalida: nao e possivel registrar presenca futura");
        }
        Matricula matricula = matriculas.vigentePorAluno(alunoId)
                .orElseThrow(() -> new RegraDeNegocioException("Check-in exige matricula ativa"));
        if (matricula.trancadaEm(data)) {
            throw new RegraDeNegocioException("Matricula trancada ate "
                    + matricula.trancadaAte().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ": check-in indisponivel");
        }
        if (matricula.pagamentoPendente(data)) {
            throw new RegraDeNegocioException("Check-in bloqueado: pagamento da matricula pendente ha mais de "
                    + Matricula.DIAS_DE_TOLERANCIA_DE_PAGAMENTO + " dias. Procure a recepcao");
        }
        if (presencas.existe(alunoId, data)) {
            throw new RegraDeNegocioException("Check-in de hoje ja foi registrado");
        }
        presencas.salvar(new Presenca(null, alunoId, data));
        publicador.publicar(EventoDeDominio.de(EventoDeDominio.PRESENCA_REGISTRADA,
                "aluno " + alunoId, aluno.nome() + " fez check-in em " + data));
    }
}
