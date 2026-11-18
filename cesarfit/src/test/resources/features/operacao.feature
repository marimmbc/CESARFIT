# language: pt
Funcionalidade: Avisos de vencimento e problemas de equipamentos

  Cenario: Aviso de vencimento e enviado uma unica vez
    Dado um aluno chamado "Iara Cruz" com CPF "81811111111"
    E que o aluno tem uma matricula "MENSAL" iniciada ha 29 dias
    Quando o sistema verificar os vencimentos
    E o sistema verificar os vencimentos
    Entao o aluno deve ter 1 aviso de vencimento

  Cenario: Matricula longe do vencimento nao recebe aviso
    Dado um aluno chamado "Joel Reis" com CPF "81811111112"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando o sistema verificar os vencimentos
    Entao o aluno deve ter 0 aviso de vencimento

  Cenario: Ocorrencia percorre aberta, em andamento e resolvida
    Quando eu registrar uma ocorrencia para o equipamento "Bicicleta 7" com a descricao "Pedal solto do lado esquerdo"
    E eu colocar a ocorrencia em andamento
    E eu resolver a ocorrencia com a solucao "Pedal reapertado e testado"
    Entao a ocorrencia deve estar "RESOLVIDA"

  Cenario: Ocorrencia nao pode ser resolvida sem estar em andamento
    Quando eu registrar uma ocorrencia para o equipamento "Esteira 9" com a descricao "Painel nao liga ao apertar start"
    E eu tentar resolver a ocorrencia com a solucao "Troca do painel"
    Entao devo receber o erro "A ocorrencia precisa estar em andamento para ser resolvida"

  Cenario: Nao duplicar ocorrencia pendente do mesmo equipamento
    Quando eu registrar uma ocorrencia para o equipamento "Remo 2" com a descricao "Banco travando no trilho"
    E eu tentar registrar uma ocorrencia para o equipamento "remo 2" com a descricao "Mesmo problema relatado de novo"
    Entao devo receber o erro "Ja existe uma ocorrencia pendente para o equipamento remo 2"

  # ---------- Regua de avisos de vencimento ----------

  Cenario: Regua completa envia cada etapa uma unica vez e marca a matricula como vencida
    Dado um aluno chamado "Lara Vidal" com CPF "71711111101"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando o sistema verificar os vencimentos 7 dias antes do fim da matricula
    E o sistema verificar os vencimentos 3 dias antes do fim da matricula
    E o sistema verificar os vencimentos 3 dias antes do fim da matricula
    E o sistema verificar os vencimentos 0 dias antes do fim da matricula
    E o sistema verificar os vencimentos 1 dias apos o fim da matricula
    E o sistema verificar os vencimentos 5 dias apos o fim da matricula
    Entao o aluno deve ter recebido as etapas "LEMBRETE, PROXIMO_DO_VENCIMENTO, VENCE_HOJE, VENCIDA, ULTIMO_AVISO"
    E o status salvo da matricula deve ser "VENCIDA"

  Cenario: Verificacao atrasada envia apenas a etapa mais urgente
    Dado um aluno chamado "Davi Mota" com CPF "71711111102"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando o sistema verificar os vencimentos 1 dias antes do fim da matricula
    Entao o aluno deve ter recebido as etapas "PROXIMO_DO_VENCIMENTO"

  Cenario: Nao ha regressao para etapa anterior ja superada
    Dado um aluno chamado "Ines Prata" com CPF "71711111103"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando o sistema verificar os vencimentos 2 dias apos o fim da matricula
    E o sistema verificar os vencimentos 0 dias antes do fim da matricula
    Entao o aluno deve ter recebido as etapas "VENCIDA"

  Cenario: Aluno que ja tem nova matricula vigente nao recebe cobranca da antiga
    Dado um aluno chamado "Otto Lemos" com CPF "71711111104"
    E que o aluno tem uma matricula "MENSAL" iniciada ha 40 dias
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando o sistema verificar os vencimentos
    Entao o aluno deve ter 0 aviso de vencimento

  Cenario: Matricula cancelada nao recebe avisos
    Dado um aluno chamado "Rita Neves" com CPF "71711111105"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu cancelar a matricula com a justificativa "Mudanca de cidade por trabalho"
    Quando o sistema verificar os vencimentos 3 dias antes do fim da matricula
    Entao o aluno deve ter 0 aviso de vencimento

  # ---------- Ocorrencias: severidade, SLA, pausa, reabertura e reincidencia ----------

  Cenario: Ocorrencia critica interdita o equipamento e estoura o SLA de 4 horas
    Quando eu registrar uma ocorrencia "CRITICA" para o equipamento "Smith 1" com a descricao "Trava de seguranca nao segura a barra"
    Entao o equipamento deve estar interditado
    E a ocorrencia nao deve estar atrasada
    Quando se passarem 5 horas
    Entao a ocorrencia deve estar atrasada

  Cenario: Ocorrencia de severidade baixa nao interdita o equipamento
    Quando eu registrar uma ocorrencia "BAIXA" para o equipamento "Banco 4" com a descricao "Estofado com pequeno rasgo lateral"
    Entao o equipamento nao deve estar interditado

  Cenario: Tempo aguardando peca nao conta no SLA
    Quando eu registrar uma ocorrencia "CRITICA" para o equipamento "Esteira 11" com a descricao "Motor parando no meio do uso"
    E eu colocar a ocorrencia em andamento
    E a ocorrencia aguardar a peca "Placa controladora do motor"
    E se passarem 6 horas
    Entao a ocorrencia nao deve estar atrasada
    Quando a peca chegar e a ocorrencia for retomada
    E se passarem 2 horas
    Entao a ocorrencia nao deve estar atrasada

  Cenario: Resolucao com troca de peca exige o custo do reparo
    Quando eu registrar uma ocorrencia "ALTA" para o equipamento "Eliptico 2" com a descricao "Pedal direito quebrado na base"
    E eu colocar a ocorrencia em andamento
    E a ocorrencia aguardar a peca "Pedal direito"
    E a peca chegar e a ocorrencia for retomada
    E eu tentar resolver a ocorrencia com a solucao "Pedal substituido"
    Entao devo receber o erro "Informe o custo do reparo: houve troca de peca (Pedal direito)"
    Quando eu resolver a ocorrencia com a solucao "Pedal substituido" e o custo "89,90"
    Entao a ocorrencia deve estar "RESOLVIDA"
    E o equipamento nao deve estar interditado

  Cenario: Ocorrencia resolvida pode ser reaberta em ate 7 dias
    Quando eu registrar uma ocorrencia para o equipamento "Bike 12" com a descricao "Banco nao fixa na altura"
    E eu colocar a ocorrencia em andamento
    E eu resolver a ocorrencia com a solucao "Pino trocado"
    E se passarem 3 dias
    E eu reabrir a ocorrencia com o motivo "Banco voltou a descer durante o uso"
    Entao a ocorrencia deve estar "ABERTA"
    E a ocorrencia deve ter 1 reabertura

  Cenario: Reabertura apos 7 dias e recusada
    Quando eu registrar uma ocorrencia para o equipamento "Bike 13" com a descricao "Banco nao fixa na altura"
    E eu colocar a ocorrencia em andamento
    E eu resolver a ocorrencia com a solucao "Pino trocado"
    E se passarem 8 dias
    E eu tentar reabrir a ocorrencia com o motivo "Banco voltou a descer durante o uso"
    Entao devo receber o erro "Prazo de 7 dias para reabertura expirado: registre uma nova ocorrencia"

  Cenario: Equipamento reincidente tem a severidade elevada automaticamente
    Dado que o equipamento "Remo 5" ja teve 2 ocorrencias resolvidas neste mes
    Quando eu registrar uma ocorrencia "BAIXA" para o equipamento "Remo 5" com a descricao "Corrente fazendo barulho de novo"
    Entao a ocorrencia deve ter severidade "MEDIA"
    E a ocorrencia deve estar marcada como reincidente
