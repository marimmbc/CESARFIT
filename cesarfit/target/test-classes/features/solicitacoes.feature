# language: pt
Funcionalidade: Solicitacoes dos alunos e feedback pos-treino

  Cenario: Equipe propoe outro horario e o aluno aceita a avaliacao fisica
    Dado um aluno matriculado chamado "Kau Lima" com CPF "81811111113" e objetivo "CONDICIONAMENTO"
    Quando o aluno solicitar avaliacao fisica para daqui a 2 dias as 10h
    E a equipe propor um novo horario para daqui a 3 dias as 15h
    E o aluno aceitar a proposta
    Entao a avaliacao deve estar "CONFIRMADA"

  Cenario: Avaliacao fora do horario de funcionamento
    Dado um aluno matriculado chamado "Lia Prado" com CPF "81811111114" e objetivo "CONDICIONAMENTO"
    Quando o aluno tentar solicitar avaliacao fisica para daqui a 2 dias as 23h
    Entao devo receber o erro "Horario fora do funcionamento da academia (6h as 21h)"

  Cenario: Avaliacao so aceita horarios redondos
    Dado um aluno matriculado chamado "Nico Sales" com CPF "81811111127" e objetivo "CONDICIONAMENTO"
    Quando o aluno tentar solicitar avaliacao fisica para daqui a 2 dias as 10:15
    Entao devo receber o erro "Escolha um horario redondo (ex.: 14:00 ou 14:30)"

  Cenario: Apenas uma avaliacao em aberto por aluno
    Dado um aluno matriculado chamado "Mel Costa" com CPF "81811111115" e objetivo "CONDICIONAMENTO"
    E o aluno solicitar avaliacao fisica para daqui a 2 dias as 10h
    Quando o aluno tentar solicitar avaliacao fisica para daqui a 4 dias as 9h
    Entao devo receber o erro "Voce ja tem uma avaliacao fisica em aberto"

  Cenario: Substituicao aprovada troca o exercicio na ficha
    Dado um aluno matriculado chamado "Noa Leal" com CPF "81811111116" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    Quando o aluno pedir a substituicao do exercicio na posicao 0 com o motivo "Sinto dor no ombro ao fazer supino"
    E a equipe aprovar a substituicao pelo exercicio "Flexao de bracos"
    Entao o exercicio na posicao 0 da ficha deve ser "Flexao de bracos"

  Cenario: Substituto precisa ser do mesmo grupo muscular
    Dado um aluno matriculado chamado "Olga Vaz" com CPF "81811111117" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 0 com o motivo "Sinto dor no ombro ao fazer supino"
    Quando a equipe tentar aprovar a substituicao pelo exercicio "Leg press"
    Entao devo receber o erro "O substituto deve trabalhar o mesmo grupo muscular (PEITO)"

  Cenario: Substituicao rejeitada com justificativa
    Dado um aluno matriculado chamado "Pia Melo" com CPF "81811111118" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 1 com o motivo "Nao gosto deste exercicio"
    Quando a equipe rejeitar a substituicao com a justificativa "Exercicio essencial para o objetivo"
    Entao a substituicao deve estar "REJEITADA"

  Cenario: Instrutor assume e conclui o atendimento
    Dado um aluno matriculado chamado "Rui Sena" com CPF "81811111119" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    Quando o aluno pedir atendimento do instrutor com o motivo "Duvida na execucao do agachamento"
    E o instrutor "Carlos" assumir o atendimento
    E o instrutor concluir o atendimento com a orientacao "Manter a coluna neutra e descer ate 90 graus"
    Entao o atendimento deve estar "CONCLUIDO"

  Cenario: Atendimento precisa ser assumido antes de concluir
    Dado um aluno matriculado chamado "Sol Brito" com CPF "81811111120" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir atendimento do instrutor com o motivo "Duvida na execucao do agachamento"
    Quando o instrutor tentar concluir o atendimento com a orientacao "Orientacao sem assumir"
    Entao devo receber o erro "O atendimento precisa ser assumido antes de ser concluido"

  Cenario: Registrar feedback depois do treino
    Dado um aluno matriculado chamado "Tais Moura" com CPF "81811111121" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    Quando o aluno registrar o feedback com esforco 7 e a observacao "Treino puxado mas consegui terminar"
    Entao o aluno deve ter 1 feedback registrado

  Cenario: Feedback exige check-in no dia
    Dado um aluno matriculado chamado "Udo Farias" com CPF "81811111122" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    Quando o aluno tentar registrar o feedback com esforco 5 e a observacao "Sem check-in"
    Entao devo receber o erro "Faca o check-in de hoje antes de registrar o feedback"

  Cenario: Apenas um feedback por dia
    Dado um aluno matriculado chamado "Vera Gil" com CPF "81811111123" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    E o aluno registrar o feedback com esforco 7 e a observacao "Primeiro registro"
    Quando o aluno tentar registrar o feedback com esforco 8 e a observacao "Segundo registro"
    Entao devo receber o erro "O feedback do treino de hoje ja foi registrado"

  # ---------- Avaliacao fisica: antecedencia, agenda, propostas, resultado e faltas ----------

  Cenario: Avaliacao exige 24 horas de antecedencia
    Dado que o relogio marca amanha as 10h
    E um aluno matriculado chamado "Ana Brito" com CPF "73711111101" e objetivo "CONDICIONAMENTO"
    Quando o aluno tentar solicitar avaliacao fisica para daqui a 0 dias as 12h
    Entao devo receber o erro "A avaliacao deve ser marcada com pelo menos 24 horas de antecedencia"

  Cenario: Dois alunos nao podem ocupar o mesmo horario do avaliador
    Dado um aluno matriculado chamado "Beto Caldas" com CPF "73711111102" e objetivo "CONDICIONAMENTO"
    E o aluno solicitar avaliacao fisica para daqui a 5 dias as 7:30
    E a equipe confirmar a avaliacao
    E outro aluno matriculado chamado "Cida Duarte" com CPF "73711111103" solicitar avaliacao fisica para daqui a 5 dias as 7:30
    Quando a equipe tentar confirmar a avaliacao
    Entao devo receber um erro contendo "Horario indisponivel: ja existe avaliacao marcada"

  Cenario: Equipe pode propor no maximo 2 horarios alternativos
    Dado um aluno matriculado chamado "Dino Ervas" com CPF "73711111104" e objetivo "CONDICIONAMENTO"
    E o aluno solicitar avaliacao fisica para daqui a 4 dias as 19h
    E a equipe propor um novo horario para daqui a 5 dias as 19h
    E o aluno recusar a proposta
    E a equipe propor um novo horario para daqui a 6 dias as 19h
    E o aluno recusar a proposta
    Quando a equipe tentar propor um novo horario para daqui a 7 dias as 19h
    Entao devo receber o erro "Limite de 2 propostas de horario atingido; confirme o horario pedido ou o aluno deve cancelar"

  Cenario: Resultado da avaliacao calcula o IMC e a classificacao
    Dado um aluno matriculado chamado "Eva Fontes" com CPF "73711111105" e objetivo "CONDICIONAMENTO"
    E o aluno solicitar avaliacao fisica para daqui a 2 dias as 18h
    E a equipe confirmar a avaliacao
    Quando a equipe tentar registrar o resultado com peso 80,0 kg e altura 1,80 m
    Entao devo receber o erro "O resultado so pode ser registrado depois do horario agendado"
    Quando chegar o horario da avaliacao
    E a equipe registrar o resultado com peso 80,0 kg e altura 1,80 m
    Entao a avaliacao deve estar "REALIZADA"
    E o IMC deve ser "24.69" com a classificacao "Peso normal"

  Cenario: Duas faltas bloqueiam novos agendamentos
    Dado um aluno matriculado chamado "Fred Gois" com CPF "73711111106" e objetivo "CONDICIONAMENTO"
    E o aluno faltou a uma avaliacao marcada para daqui a 2 dias as 6h
    E o aluno faltou a uma avaliacao marcada para daqui a 2 dias as 6:30
    Quando o aluno tentar solicitar avaliacao fisica para daqui a 3 dias as 20h
    Entao devo receber o erro "Agendamento de avaliacao bloqueado por 2 faltas nos ultimos 90 dias"

  Cenario: Avaliacao confirmada nao pode ser cancelada em cima da hora
    Dado um aluno matriculado chamado "Gil Hora" com CPF "73711111107" e objetivo "CONDICIONAMENTO"
    E o aluno solicitar avaliacao fisica para daqui a 2 dias as 17h
    E a equipe confirmar a avaliacao
    Quando faltar 1 hora para a avaliacao
    E o aluno tentar cancelar a avaliacao
    Entao devo receber o erro "Avaliacao confirmada so pode ser cancelada ate 2 horas antes"

  # ---------- Substituicao: limites, sugestoes, aprovacao automatica, prioridade e expiracao ----------

  Cenario: Troca por preferencia so uma vez por ficha
    Dado um aluno matriculado chamado "Hana Iris" com CPF "73711111108" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 0 por "PREFERENCIA" com o motivo "Prefiro treinar sem barra"
    E a equipe aprovar a substituicao pelo exercicio "Flexao de bracos"
    Quando o aluno tentar pedir a substituicao do exercicio na posicao 3 por "PREFERENCIA" com o motivo "Nao gosto da remada curvada"
    Entao devo receber o erro "Troca por preferencia permitida apenas uma vez por ficha"

  Cenario: No maximo 3 trocas aprovadas por ficha
    Dado um aluno matriculado chamado "Ivo Jacome" com CPF "73711111109" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 0 por "DOR_OU_LESAO" com o motivo "Dor no ombro direito"
    E a equipe aprovar a substituicao pelo exercicio "Flexao de bracos"
    E o aluno pedir a substituicao do exercicio na posicao 3 por "DOR_OU_LESAO" com o motivo "Dor na lombar ao inclinar"
    E a equipe aprovar a substituicao pelo exercicio "Remada baixa"
    E o aluno pedir a substituicao do exercicio na posicao 6 por "DOR_OU_LESAO" com o motivo "Dor no joelho ao agachar"
    E a equipe aprovar a substituicao pelo exercicio "Afundo com halteres"
    Quando o aluno tentar pedir a substituicao do exercicio na posicao 7 por "DOR_OU_LESAO" com o motivo "Dor no joelho no leg press"
    Entao devo receber o erro "Limite de 3 substituicoes aprovadas por ficha atingido: peca uma nova ficha ao instrutor"

  Cenario: Substituto nao pode repetir exercicio que ja esta na ficha
    Dado um aluno matriculado chamado "Jana Klein" com CPF "73711111110" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 0 por "DIFICULDADE_TECNICA" com o motivo "Nao consigo estabilizar a barra"
    Quando a equipe tentar aprovar a substituicao pelo exercicio "Supino inclinado com halteres"
    Entao devo receber o erro "O exercicio Supino inclinado com halteres ja esta na ficha do aluno"

  Cenario: Sistema sugere substitutos do mesmo grupo que nao estao na ficha
    Dado um aluno matriculado chamado "Kaio Lobo" com CPF "73711111111" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    Quando o aluno pedir a substituicao do exercicio na posicao 0 por "DOR_OU_LESAO" com o motivo "Dor no ombro ao empurrar a barra"
    Entao as sugestoes devem incluir "Flexao de bracos"
    E as sugestoes nao devem incluir "Supino inclinado com halteres"

  Cenario: Equipamento interditado aprova a troca automaticamente
    Dado um aluno matriculado chamado "Lia Moura" com CPF "73711111112" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu registrar uma ocorrencia "CRITICA" para o equipamento "Polia alta" com a descricao "Cabo de aco desfiando na roldana"
    Quando o aluno pedir a substituicao do exercicio na posicao 2 por "EQUIPAMENTO_INDISPONIVEL" com o motivo "A polia esta interditada hoje"
    Entao a substituicao deve estar "APROVADA"
    E a substituicao deve ter sido aprovada automaticamente
    E o exercicio na posicao 2 da ficha nao deve ser "Triceps na polia"
    E eu colocar a ocorrencia em andamento
    E eu resolver a ocorrencia com a solucao "Cabo de aco trocado"

  Cenario: Equipamento liberado volta a exigir aprovacao da equipe
    Dado um aluno matriculado chamado "Luca Mendes" com CPF "73711111133" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu registrar uma ocorrencia "ALTA" para o equipamento "Barra e rack" com a descricao "Trava do rack solta no lado direito"
    E eu colocar a ocorrencia em andamento
    E eu resolver a ocorrencia com a solucao "Trava substituida e testada"
    Quando o aluno pedir a substituicao do exercicio na posicao 6 por "EQUIPAMENTO_INDISPONIVEL" com o motivo "Achei que o rack estava quebrado"
    Entao a substituicao deve estar "PENDENTE"

  Cenario: Pedido por dor ou lesao fica a frente na fila da equipe
    Dado um aluno matriculado chamado "Mara Nobre" com CPF "73711111113" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 4 por "PREFERENCIA" com o motivo "Quero variar o exercicio de costas"
    E outro aluno com ficha chamado "Nuno Ortiz" com CPF "73711111114" pedir a substituicao da posicao 4 por "DOR_OU_LESAO" com o motivo "Dor no cotovelo ao puxar"
    Entao o pedido de "Nuno Ortiz" deve aparecer antes do pedido de "Mara Nobre" na fila

  Cenario: Pedido de troca expira em 7 dias
    Dado um aluno matriculado chamado "Olivia Paz" com CPF "73711111115" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E o aluno pedir a substituicao do exercicio na posicao 0 com o motivo "Sinto dor no ombro ao fazer supino"
    Quando se passarem 8 dias
    E a equipe tentar aprovar a substituicao pelo exercicio "Flexao de bracos"
    Entao devo receber o erro "Solicitacao expirada: o aluno deve fazer um novo pedido"

  # ---------- Atendimento do instrutor: fila priorizada, SLA, capacidade e avaliacao ----------

  Cenario: Atendimento por dor passa na frente e bloqueia o atendimento normal
    Dado um aluno com ficha chamado "Paula Quintas" com CPF "73711111116" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na execucao da remada"
    E um aluno com ficha chamado "Rafa Sales" com CPF "73711111117" pede atendimento "DOR_OU_LESAO" com o motivo "Dor forte no joelho esquerdo"
    Quando o instrutor "Bia" tentar assumir o atendimento de "Paula Quintas"
    Entao devo receber o erro "Ha atendimento prioritario (dor ou lesao) aguardando na fila"
    E o atendimento de "Rafa Sales" deve ser o 1o da fila com prioridade "ALTA"

  Cenario: Instrutor atende no maximo 3 alunos ao mesmo tempo
    Dado um aluno com ficha chamado "Sara Tome" com CPF "73711111118" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na execucao do supino"
    E um aluno com ficha chamado "Teo Uchoa" com CPF "73711111119" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na execucao do agachamento"
    E um aluno com ficha chamado "Ugo Vale" com CPF "73711111120" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na regulagem do banco"
    E um aluno com ficha chamado "Vivi Xavier" com CPF "73711111121" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na execucao da puxada"
    E o instrutor "Caio" assumir o atendimento de "Sara Tome"
    E o instrutor "Caio" assumir o atendimento de "Teo Uchoa"
    E o instrutor "Caio" assumir o atendimento de "Ugo Vale"
    Quando o instrutor "Caio" tentar assumir o atendimento de "Vivi Xavier"
    Entao devo receber o erro "O instrutor Caio ja tem 3 atendimentos em andamento"

  Cenario: Atendimento sem resposta fica atrasado conforme a prioridade
    Dado um aluno com ficha chamado "Wil Yung" com CPF "73711111122" pede atendimento "DOR_OU_LESAO" com o motivo "Dor no punho ao apoiar"
    Quando se passarem 11 minutos
    Entao o atendimento de "Wil Yung" deve estar atrasado

  Cenario: Aluno avalia o atendimento uma unica vez e nota baixa exige comentario
    Dado um aluno com ficha chamado "Xena Zago" com CPF "73711111123" pede atendimento "DUVIDA_EXECUCAO" com o motivo "Duvida na execucao do stiff"
    E o instrutor "Dani" assumir o atendimento de "Xena Zago"
    E o instrutor concluir o atendimento com a orientacao "Quadril para tras e coluna neutra"
    Quando o aluno tentar avaliar o atendimento com nota 2 e comentario ""
    Entao devo receber o erro "Comentario (obrigatorio para nota 1 ou 2) deve ter pelo menos 10 caracteres"
    Quando o aluno avaliar o atendimento com nota 5 e comentario "Explicou muito bem"
    E o aluno tentar avaliar o atendimento com nota 4 e comentario ""
    Entao devo receber o erro "Este atendimento ja foi avaliado"

  Cenario: Aluno cancela o pedido enquanto o instrutor nao assumiu
    Dado um aluno com ficha chamado "Yuri Abreu" com CPF "73711111124" pede atendimento "OUTRO" com o motivo "Quero ajuda para montar o aquecimento"
    Quando o aluno cancelar o atendimento
    Entao o atendimento deve estar "CANCELADO"

  Cenario: Transferencia exige outro instrutor
    Dado um aluno com ficha chamado "Zeca Borba" com CPF "73711111125" pede atendimento "AJUSTE_DE_EQUIPAMENTO" com o motivo "Banco do supino desregulado"
    E o instrutor "Ana" assumir o atendimento de "Zeca Borba"
    Quando o atendimento for transferido para "Ana"
    Entao devo receber o erro "Escolha um instrutor diferente do atual"
    Quando o atendimento for transferido para "Beto"
    Entao o atendimento deve estar com o instrutor "Beto"

  # ---------- Feedback pos-treino: carga, tendencia, ACWR e dor ----------

  Cenario: Feedback calcula a carga do treino pelo metodo sRPE
    Dado um aluno matriculado chamado "Abel Cruz" com CPF "73711111126" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    Quando o aluno registrar o feedback com esforco 7 por 50 minutos
    Entao a carga do treino deve ser 350
    E o alerta de carga deve ser "NENHUM"

  Cenario: Tres treinos seguidos no limite geram alerta de sobrecarga
    Dado um aluno matriculado chamado "Bia Dutra" com CPF "73711111127" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    E que o aluno registrou treinos com esforco 9 de 60 minutos nos ultimos 2 dias
    Quando o aluno registrar o feedback com esforco 10 por 60 minutos
    Entao o alerta de carga deve ser "SOBRECARGA"

  Cenario: Salto de carga em relacao a media do mes indica risco de lesao
    Dado um aluno matriculado chamado "Caca Elias" com CPF "73711111128" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    E que o aluno registrou 4 treinos leves entre 8 e 27 dias atras
    Quando o aluno registrar o feedback com esforco 10 por 120 minutos
    Entao o alerta de carga deve ser "RISCO_DE_LESAO"
    E o ACWR deve ser maior que 1,5

  Cenario: Treinos leves com a ficha completa sugerem progressao
    Dado um aluno matriculado chamado "Dede Faro" com CPF "73711111129" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir todos os itens da ficha
    E que o aluno ja fez o check-in de hoje
    E que o aluno registrou treinos com esforco 2 de 60 minutos nos ultimos 2 dias
    Quando o aluno registrar o feedback com esforco 3 por 60 minutos
    Entao o alerta de carga deve ser "PROGRESSAO_SUGERIDA"

  Cenario: Relato de dor abre atendimento prioritario automaticamente
    Dado um aluno matriculado chamado "Edu Galvao" com CPF "73711111130" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    Quando o aluno registrar o feedback com esforco 6 por 45 minutos e dor no "joelho direito"
    Entao o alerta de carga deve ser "DOR_RELATADA"
    E o aluno deve ter um atendimento aberto automaticamente com prioridade "ALTA"

  Cenario: Dor sem local informado e recusada
    Dado um aluno matriculado chamado "Fani Hess" com CPF "73711111131" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    Quando o aluno tentar registrar o feedback com esforco 6 por 45 minutos e dor no ""
    Entao devo receber o erro "Local da dor deve ter pelo menos 3 caracteres"

  Cenario: Duracao do treino fora do limite e recusada
    Dado um aluno matriculado chamado "Gui Iorio" com CPF "73711111132" e objetivo "HIPERTROFIA"
    E que a ficha de treino do aluno foi gerada
    E eu concluir o item na posicao 0
    E que o aluno ja fez o check-in de hoje
    Quando o aluno tentar registrar o feedback com esforco 6 por 300 minutos
    Entao devo receber o erro "A duracao do treino deve ser de 10 a 240 minutos"
