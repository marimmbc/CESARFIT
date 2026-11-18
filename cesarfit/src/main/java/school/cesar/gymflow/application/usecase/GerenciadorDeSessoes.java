package school.cesar.gymflow.application.usecase;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import school.cesar.gymflow.domain.acesso.model.Perfil;
import school.cesar.gymflow.domain.acesso.model.TentativasDeAcesso;
import school.cesar.gymflow.domain.shared.Relogio;

/**
 * Mantem as sessoes abertas apos o login (token -> quem entrou) e as tentativas de acesso.
 * Uma sessao expira apos 30 minutos sem uso ou, no maximo, 8 horas depois do login.
 */
@Component
public class GerenciadorDeSessoes {

    public static final int HORAS_DE_VALIDADE = 8;
    public static final int MINUTOS_DE_INATIVIDADE = 30;

    public record SessaoAtiva(String token, Perfil perfil, Long alunoId, String nome, LocalDateTime expiraEm,
                              LocalDateTime ultimoUso) {
    }

    private final Map<String, SessaoAtiva> sessoes = new ConcurrentHashMap<>();
    private final Map<String, TentativasDeAcesso> tentativas = new ConcurrentHashMap<>();
    private final Relogio relogio;

    public GerenciadorDeSessoes(Relogio relogio) {
        this.relogio = relogio;
    }

    public SessaoAtiva abrir(Perfil perfil, Long alunoId, String nome) {
        String token = UUID.randomUUID().toString();
        LocalDateTime agora = relogio.agora();
        SessaoAtiva sessao = new SessaoAtiva(token, perfil, alunoId, nome, agora.plusHours(HORAS_DE_VALIDADE), agora);
        sessoes.put(token, sessao);
        return sessao;
    }

    /** Busca a sessao e renova o tempo de inatividade (expiracao deslizante). */
    public Optional<SessaoAtiva> buscar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessaoAtiva sessao = sessoes.get(token);
        if (sessao == null) {
            return Optional.empty();
        }
        LocalDateTime agora = relogio.agora();
        if (agora.isAfter(sessao.expiraEm()) || agora.isAfter(sessao.ultimoUso().plusMinutes(MINUTOS_DE_INATIVIDADE))) {
            sessoes.remove(token);
            return Optional.empty();
        }
        SessaoAtiva renovada = new SessaoAtiva(sessao.token(), sessao.perfil(), sessao.alunoId(), sessao.nome(),
                sessao.expiraEm(), agora);
        sessoes.put(token, renovada);
        return Optional.of(renovada);
    }

    public void encerrar(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }

    public TentativasDeAcesso tentativasDe(String numero) {
        return tentativas.computeIfAbsent(numero.trim().toUpperCase(), n -> new TentativasDeAcesso());
    }
}
