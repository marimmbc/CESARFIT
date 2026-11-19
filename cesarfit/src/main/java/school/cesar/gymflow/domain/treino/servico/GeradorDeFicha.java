package school.cesar.gymflow.domain.treino.servico;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import school.cesar.gymflow.domain.matricula.model.Objetivo;
import school.cesar.gymflow.domain.shared.RegraDeNegocioException;
import school.cesar.gymflow.domain.treino.model.Exercicio;
import school.cesar.gymflow.domain.treino.model.FichaDeTreino;
import school.cesar.gymflow.domain.treino.model.GrupoMuscular;
import school.cesar.gymflow.domain.treino.model.ItemDeTreino;

/**
 * Padrao Template Method: o algoritmo de geracao e fixo (validar, preencher as vagas com
 * exercicios ATIVOS do catalogo, montar a ficha); cada objetivo define suas vagas e a validade.
 * Toda ficha nasce com a divisao padrao de treino A / B / C.
 * O contexto Treino recebe apenas a identidade do aluno e o objetivo (conformista em relacao a
 * Matricula), sem depender da entidade Aluno de outro agregado.
 */
public abstract class GeradorDeFicha {

    /** Uma vaga da ficha: grupo, exercicio preferido e a carga de treino. */
    protected record Vaga(String divisao, GrupoMuscular grupo, String preferido, int series, int repeticoes,
                          int descanso) {
    }

    public final FichaDeTreino gerar(Long alunoId, Objetivo objetivo, List<Exercicio> catalogo) {
        validar(alunoId, objetivo);
        List<Exercicio> ativos = catalogo == null ? List.of() : catalogo.stream()
                .filter(Exercicio::ativo)
                .sorted(Comparator.comparing(Exercicio::id, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ItemDeTreino> itens = new ArrayList<>();
        Set<String> usados = new HashSet<>();
        for (Vaga vaga : vagas()) {
            escolher(vaga, ativos, usados).ifPresent(ex -> {
                usados.add(ex.nome().toLowerCase());
                itens.add(new ItemDeTreino(ex.nome(), vaga.grupo(), vaga.series(), vaga.repeticoes(),
                        vaga.descanso(), vaga.divisao()));
            });
        }
        if (itens.isEmpty()) {
            throw new RegraDeNegocioException("Ficha gerada sem exercicios: cadastre exercicios ativos no catalogo");
        }
        LocalDate hoje = LocalDate.now();
        FichaDeTreino ficha = new FichaDeTreino(null, alunoId, objetivo,
                hoje, hoje.plusWeeks(semanasDeValidade()), itens);
        aoFinalizar(ficha);
        return ficha;
    }

    private static Optional<Exercicio> escolher(Vaga vaga, List<Exercicio> ativos, Set<String> usados) {
        Optional<Exercicio> preferido = ativos.stream()
                .filter(e -> e.grupoMuscular() == vaga.grupo() && e.mesmoNome(vaga.preferido()))
                .filter(e -> !usados.contains(e.nome().toLowerCase()))
                .findFirst();
        return preferido.isPresent() ? preferido : ativos.stream()
                .filter(e -> e.grupoMuscular() == vaga.grupo())
                .filter(e -> !usados.contains(e.nome().toLowerCase()))
                .findFirst();
    }

    protected void validar(Long alunoId, Objetivo objetivo) {
        if (alunoId == null || objetivo == null) {
            throw new RegraDeNegocioException("Aluno invalido para geracao de ficha");
        }
    }

    protected abstract List<Vaga> vagas();

    protected abstract int semanasDeValidade();

    protected void aoFinalizar(FichaDeTreino ficha) {
        // por padrao, nada
    }
}
