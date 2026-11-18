package school.cesar.gymflow.domain.matricula.repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import school.cesar.gymflow.domain.matricula.model.EtapaDeAviso;
import school.cesar.gymflow.domain.matricula.model.NotificacaoDeVencimento;

/** Porta de saida (arquitetura limpa). */
public interface RepositorioDeNotificacoes {

    NotificacaoDeVencimento salvar(NotificacaoDeVencimento notificacao);

    Optional<NotificacaoDeVencimento> porId(Long id);

    /** Etapas da regua ja enviadas para a matricula naquele vencimento. */
    Set<EtapaDeAviso> etapasEnviadas(Long matriculaId, LocalDate vencimento);

    List<NotificacaoDeVencimento> porAluno(Long alunoId);

    List<NotificacaoDeVencimento> todas();
}
