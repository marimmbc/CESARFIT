# language: pt
Funcionalidade: Acesso ao sistema e area do aluno

  Cenario: Aluno entra com o numero da matricula e a senha de primeiro acesso
    Dado um aluno matriculado chamado "Lia Rocha" com CPF "31313131311" e objetivo "HIPERTROFIA"
    Quando o aluno entrar com o numero da matricula e a senha "313131"
    Entao o acesso deve ser liberado com o perfil "ALUNO"

  Cenario: Senha incorreta nao libera o acesso
    Dado um aluno matriculado chamado "Caio Reis" com CPF "42424242422" e objetivo "EMAGRECIMENTO"
    Quando o aluno entrar com o numero da matricula e a senha "000000"
    Entao devo receber o erro "Numero da matricula ou senha invalidos"

  Cenario: Aluno troca a senha e entra com a nova senha
    Dado um aluno matriculado chamado "Nina Paz" com CPF "53535353533" e objetivo "CONDICIONAMENTO"
    Quando o aluno trocar a senha "535353" pela nova senha "treino2026"
    E o aluno entrar com o numero da matricula e a senha "treino2026"
    Entao o acesso deve ser liberado com o perfil "ALUNO"

  Cenario: Membro da equipe entra com o seu numero de acesso
    Dado um membro da equipe com numero "EQ900" e senha "recepcao1"
    Quando alguem entrar com o numero "EQ900" e a senha "recepcao1"
    Entao o acesso deve ser liberado com o perfil "EQUIPE"

  Cenario: Check-in do dia acende o foguinho de frequencia
    Dado um aluno matriculado chamado "Leo Dias" com CPF "64646464644" e objetivo "HIPERTROFIA"
    Quando o aluno fizer o check-in de hoje
    Entao a sequencia de dias seguidos deve ser 1
    E a presenca de hoje deve aparecer na semana

  Cenario: Apenas um check-in por dia
    Dado um aluno matriculado chamado "Bia Luz" com CPF "75757575755" e objetivo "EMAGRECIMENTO"
    E que o aluno ja fez o check-in de hoje
    Quando o aluno tentar fazer o check-in de hoje novamente
    Entao devo receber o erro "Check-in de hoje ja foi registrado"

  # ---------- Regras ampliadas de acesso ----------

  Cenario: Cinco senhas erradas bloqueiam o acesso por 15 minutos
    Dado um aluno matriculado chamado "Gael Bento" com CPF "63611111101" e objetivo "HIPERTROFIA"
    Quando o aluno errar a senha 5 vezes
    E o aluno entrar com o numero da matricula e a senha "636111"
    Entao devo receber o erro "Acesso bloqueado por 5 tentativas invalidas. Tente de novo em 15 minuto(s)"
    Quando se passarem 16 minutos
    E o aluno entrar com o numero da matricula e a senha "636111"
    Entao o acesso deve ser liberado com o perfil "ALUNO"

  Cenario: Primeiro acesso pede a troca de senha
    Dado um aluno matriculado chamado "Fia Coelho" com CPF "63611111102" e objetivo "HIPERTROFIA"
    Quando o aluno entrar com o numero da matricula e a senha "636111"
    Entao o sistema deve pedir a troca da senha

  Cenario: Senha nova precisa ter letras e numeros
    Dado um aluno matriculado chamado "Eli Duarte" com CPF "63611111103" e objetivo "HIPERTROFIA"
    Quando o aluno tentar trocar a senha "636111" pela nova senha "somenteletras"
    Entao devo receber o erro "A nova senha deve ter letras e numeros"

  Cenario: Senha nova nao pode conter o CPF
    Dado um aluno matriculado chamado "Dina Esteves" com CPF "63611111104" e objetivo "HIPERTROFIA"
    Quando o aluno tentar trocar a senha "636111" pela nova senha "abc636111x"
    Entao devo receber o erro "A nova senha nao pode conter os digitos do seu CPF"

  Cenario: Sessao expira depois de 30 minutos sem uso
    Dado um aluno matriculado chamado "Caio Freire" com CPF "63611111105" e objetivo "HIPERTROFIA"
    E o aluno entrar e guardar a sessao
    Quando se passarem 20 minutos
    Entao a sessao deve continuar valida
    Quando se passarem 20 minutos
    Entao a sessao deve continuar valida
    Quando se passarem 31 minutos
    Entao a sessao deve ter expirado

  Cenario: Matricula vencida ha mais de 30 dias nao entra
    Dado um aluno matriculado chamado "Bela Gusmao" com CPF "63611111106" e objetivo "HIPERTROFIA"
    Quando chegar o dia 31 apos o fim da matricula
    E o aluno entrar com o numero da matricula e a senha "636111"
    Entao devo receber o erro "Matricula vencida ha mais de 30 dias: procure a recepcao para renovar"

  # ---------- Regras ampliadas de check-in ----------

  Cenario: Check-in fora do horario de funcionamento
    Dado um aluno matriculado chamado "Abner Hora" com CPF "63611111107" e objetivo "HIPERTROFIA"
    E que o relogio marca hoje as 23h
    Quando o aluno tentar fazer o check-in
    Entao devo receber o erro "Check-in permitido apenas das 6h as 22h"

  Cenario: Inadimplencia bloqueia o check-in depois da tolerancia
    Dado um aluno matriculado chamado "Zara Ilha" com CPF "63611111108" e objetivo "HIPERTROFIA"
    Quando se passarem 6 dias
    E o aluno tentar fazer o check-in
    Entao devo receber o erro "Check-in bloqueado: pagamento da matricula pendente ha mais de 5 dias. Procure a recepcao"
    Quando eu registrar um pagamento de "120,00" via "PIX"
    E o aluno fizer o check-in de hoje
    Entao o check-in do dia deve estar registrado
