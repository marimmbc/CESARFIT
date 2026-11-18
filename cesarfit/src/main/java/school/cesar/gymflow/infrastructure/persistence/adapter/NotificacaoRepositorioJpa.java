package school.cesar.gymflow.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import school.cesar.gymflow.domain.matricula.model.EtapaDeAviso;
import school.cesar.gymflow.domain.matricula.model.NotificacaoDeVencimento;
import school.cesar.gymflow.domain.matricula.repositorio.RepositorioDeNotificacoes;
import school.cesar.gymflow.infrastructure.persistence.entity.NotificacaoEntity;
import school.cesar.gymflow.infrastructure.persistence.springdata.NotificacaoJpaRepository;

/** Adaptador de saida das notificacoes de vencimento. */
@Repository
public class NotificacaoRepositorioJpa implements RepositorioDeNotificacoes {

    private final NotificacaoJpaRepository jpa;

    public NotificacaoRepositorioJpa(NotificacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public NotificacaoDeVencimento salvar(NotificacaoDeVencimento n) {
        NotificacaoEntity e = new NotificacaoEntity();
        e.id = n.id();
        e.matriculaId = n.matriculaId();
        e.alunoId = n.alunoId();
        e.vencimento = n.vencimento();
        e.etapa = n.etapa();
        e.enviadaEm = n.enviadaEm();
        e.mensagem = n.mensagem();
        e.lida = n.lida();
        return paraDominio(jpa.save(e));
    }

    @Override
    public Optional<NotificacaoDeVencimento> porId(Long id) {
        return jpa.findById(id).map(NotificacaoRepositorioJpa::paraDominio);
    }

    @Override
    public Set<EtapaDeAviso> etapasEnviadas(Long matriculaId, LocalDate vencimento) {
        return jpa.findByMatriculaIdAndVencimento(matriculaId, vencimento).stream()
                .map(e -> e.etapa == null ? EtapaDeAviso.PROXIMO_DO_VENCIMENTO : e.etapa)
                .collect(Collectors.toSet());
    }

    @Override
    public List<NotificacaoDeVencimento> porAluno(Long alunoId) {
        return jpa.findByAlunoIdOrderByEnviadaEmDesc(alunoId).stream().map(NotificacaoRepositorioJpa::paraDominio).toList();
    }

    @Override
    public List<NotificacaoDeVencimento> todas() {
        return jpa.findAllByOrderByEnviadaEmDesc().stream().map(NotificacaoRepositorioJpa::paraDominio).toList();
    }

    private static NotificacaoDeVencimento paraDominio(NotificacaoEntity e) {
        return new NotificacaoDeVencimento(e.id, e.matriculaId, e.alunoId, e.vencimento, e.etapa, e.enviadaEm, e.mensagem, e.lida);
    }
}
