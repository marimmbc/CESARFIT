package school.cesar.gymflow.application.usecase;

import java.time.LocalDate;
import java.util.List;

import school.cesar.gymflow.application.dto.Respostas;
import school.cesar.gymflow.domain.matricula.model.Aluno;
import school.cesar.gymflow.domain.matricula.model.Matricula;

/** Converte agregados do dominio em respostas para a interface (uso interno da aplicacao). */
final class Resumos {

    private Resumos() {
    }

    static Respostas.AlunoResumo aluno(Aluno a, List<Matricula> matriculasDoAluno) {
        boolean ativa = matriculasDoAluno.stream().anyMatch(Matricula::ativa);
        return new Respostas.AlunoResumo(a.id(), a.nome(), a.cpf().formatado(), a.telefoneFormatado(),
                a.email(), a.dataDeNascimento(), a.idade(), a.objetivo().name(), ativa,
                matriculasDoAluno.size(), a.menorDeIdade(), a.responsavelNome(),
                a.responsavelCpf() == null ? null : a.responsavelCpf().formatado());
    }

    static Respostas.MatriculaResumo matricula(Matricula m, String alunoNome) {
        LocalDate hoje = LocalDate.now();
        m.atualizarVigencia(hoje);
        List<Respostas.PagamentoResumo> pagamentos = new java.util.ArrayList<>();
        for (int i = 0; i < m.pagamentos().size(); i++) {
            var p = m.pagamentos().get(i);
            pagamentos.add(new Respostas.PagamentoResumo(i, p.valor().toString(), p.data(), p.formaDePagamento(),
                    p.estornado(), p.motivoDoEstorno(), p.dataDoEstorno(), p.valorEstornado().toString(),
                    p.saldoEstornavel().toString(), p.parcialmenteEstornado(),
                    p.categoriaDoEstorno() == null ? null : p.categoriaDoEstorno().name(),
                    p.multa().toString(), p.juros().toString(), p.parcelas(), p.troco().toString()));
        }
        return new Respostas.MatriculaResumo(m.id(), m.alunoId(), alunoNome, m.modalidade().name(),
                m.descricaoDoPlano(), m.valorMensal().toString(), m.valorMensal().valor(),
                m.inicio(), m.fim(), m.status().name(), m.totalPago().toString(), pagamentos,
                m.justificativaDoCancelamento(), m.dataDaSolicitacaoDeCancelamento(),
                m.taxaDeCancelamento() == null ? null : m.taxaDeCancelamento().toString(),
                m.valorDevido(hoje).toString(), m.multaEm(hoje).toString(), m.jurosEm(hoje).toString(),
                m.diasDeAtraso(hoje), m.creditoDeTroca().toString(), m.pagamentoPendente(hoje), m.trancadaEm(hoje),
                m.trancadaAte(), m.diasTrancados(), m.trancamentos());
    }
}
