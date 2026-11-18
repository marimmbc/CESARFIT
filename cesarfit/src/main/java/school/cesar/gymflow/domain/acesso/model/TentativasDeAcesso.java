package school.cesar.gymflow.domain.acesso.model;

import java.time.LocalDateTime;

import school.cesar.gymflow.domain.shared.RegraDeNegocioException;

/**
 * Controle de forca bruta por numero de acesso: 5 senhas erradas seguidas bloqueiam
 * novas tentativas por 15 minutos. Um acesso correto zera o contador.
 */
public class TentativasDeAcesso {

    public static final int LIMITE_DE_FALHAS = 5;
    public static final int MINUTOS_DE_BLOQUEIO = 15;

    private int falhas;
    private LocalDateTime bloqueadoAte;

    public void exigirLiberado(LocalDateTime agora) {
        if (bloqueadoAte != null && agora.isBefore(bloqueadoAte)) {
            long segundos = java.time.Duration.between(agora, bloqueadoAte).getSeconds();
            long minutos = Math.max(1, (segundos + 59) / 60);
            throw new RegraDeNegocioException("Acesso bloqueado por " + LIMITE_DE_FALHAS
                    + " tentativas invalidas. Tente de novo em " + minutos + " minuto(s)");
        }
        if (bloqueadoAte != null) {
            bloqueadoAte = null;
            falhas = 0;
        }
    }

    /** Retorna true se esta falha causou o bloqueio. */
    public boolean registrarFalha(LocalDateTime agora) {
        falhas++;
        if (falhas >= LIMITE_DE_FALHAS) {
            bloqueadoAte = agora.plusMinutes(MINUTOS_DE_BLOQUEIO);
            return true;
        }
        return false;
    }

    public void registrarSucesso() {
        falhas = 0;
        bloqueadoAte = null;
    }

    public int tentativasRestantes() {
        return Math.max(0, LIMITE_DE_FALHAS - falhas);
    }
}
