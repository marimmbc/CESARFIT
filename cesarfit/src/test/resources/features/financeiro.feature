# language: pt
Funcionalidade: Cancelamento, estorno e contestacao de cobranca

  Cenario: Cancelar matricula com justificativa registra a data da solicitacao
    Dado um aluno chamado "Ari Matos" com CPF "81811111103"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu cancelar a matricula com a justificativa "Mudanca de cidade por trabalho"
    Entao o status da matricula deve ser "CANCELADA"
    E a justificativa e a data da solicitacao devem estar registradas

  Cenario: Cancelamento com 30 dias ou mais de antecedencia nao tem taxa
    Dado um aluno chamado "Iuri Bento" com CPF "81811111124"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu cancelar a matricula com a justificativa "Vou treinar em casa a partir de agora"
    Entao nao deve haver taxa de cancelamento

  Cenario: Cancelamento com menos de 30 dias do fim do contrato cobra uma mensalidade
    Dado um aluno chamado "Jade Rocha" com CPF "81811111125"
    E que o aluno tem uma matricula "MENSAL" iniciada ha 10 dias
    Quando eu cancelar a matricula com a justificativa "Mudanca de cidade por trabalho"
    Entao a taxa de cancelamento deve ser "R$ 120,00"

  Cenario: Taxa do plano anual equivale a uma mensalidade de R$ 110,00
    Dado um aluno chamado "Kiko Alves" com CPF "81811111126"
    E que o aluno tem uma matricula "ANUAL" iniciada ha 350 dias
    Quando eu cancelar a matricula com a justificativa "Contrato terminando e nao vou renovar"
    Entao a taxa de cancelamento deve ser "R$ 110,00"

  Cenario: Cancelamento sem justificativa e recusado
    Dado um aluno chamado "Bia Torres" com CPF "81811111104"
    E que o aluno ja foi matriculado no plano "ANUAL"
    Quando eu tentar cancelar a matricula com a justificativa ""
    Entao devo receber o erro "Justificativa do cancelamento deve ter pelo menos 10 caracteres"

  Cenario: Edicao nao cancela matricula sem justificativa
    Dado um aluno chamado "Ciro Paz" com CPF "81811111105"
    E que o aluno ja foi matriculado no plano "MENSAL"
    Quando eu tentar alterar a situacao da matricula para "CANCELADA"
    Entao devo receber o erro "Para cancelar, use o cancelamento com justificativa"

  Cenario: Estornar um pagamento
    Dado um aluno chamado "Dora Lins" com CPF "81811111106"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando eu estornar o pagamento 0 com o motivo "Cobranca em duplicidade no cartao"
    Entao o pagamento 0 deve estar estornado
    E o total pago da matricula deve ser zero

  Cenario: Estorno duplicado e impedido
    Dado um aluno chamado "Ed Nunes" com CPF "81811111107"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E eu estornar o pagamento 0 com o motivo "Cobranca em duplicidade no cartao"
    Quando eu tentar estornar o pagamento 0 com o motivo "Segunda tentativa de estorno"
    Entao devo receber o erro "Pagamento ja foi estornado"

  Cenario: Contestacao procedente estorna o pagamento
    Dado um aluno chamado "Fabi Rios" com CPF "81811111108"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "CARTAO"
    Quando o aluno contestar o pagamento 0 com o motivo "Fui cobrado duas vezes neste mes"
    E a equipe analisar a contestacao como "procedente" com o parecer "Cobranca duplicada confirmada no extrato"
    Entao a contestacao deve estar "PROCEDENTE"
    E o pagamento 0 deve estar estornado

  Cenario: Contestacao improcedente mantem o pagamento
    Dado um aluno chamado "Guto Sa" com CPF "81811111109"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando o aluno contestar o pagamento 0 com o motivo "Nao reconheco esta cobranca"
    E a equipe analisar a contestacao como "improcedente" com o parecer "Pagamento confere com o plano contratado"
    Entao a contestacao deve estar "IMPROCEDENTE"
    E o pagamento 0 nao deve estar estornado

  Cenario: Apenas uma contestacao em analise por pagamento
    Dado um aluno chamado "Hel Dias" com CPF "81811111110"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E o aluno contestar o pagamento 0 com o motivo "Fui cobrado duas vezes neste mes"
    Quando o aluno tentar contestar o pagamento 0 com o motivo "Insistindo na mesma cobranca"
    Entao devo receber o erro "Ja existe uma contestacao em analise para este pagamento"

  # ---------- Estorno: parcial, prazo, duplicidade e arrependimento ----------

  Cenario: Estorno parcial reduz o total pago e limita o saldo estornavel
    Dado um aluno chamado "Alex Brum" com CPF "72711111101"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando eu estornar "20,00" do pagamento 0 na categoria "VALOR_INCORRETO" com o motivo "Cobrado acima do valor do plano"
    Entao o total pago da matricula deve ser "R$ 100,00"
    E o pagamento 0 deve estar parcialmente estornado
    Quando eu tentar estornar "110,00" do pagamento 0 na categoria "VALOR_INCORRETO" com o motivo "Tentando estornar mais que o saldo"
    Entao devo receber o erro "Valor do estorno excede o saldo estornavel de R$ 100,00"

  Cenario: Estorno apos 90 dias do pagamento e recusado
    Dado um aluno chamado "Bela Cunha" com CPF "72711111102"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando se passarem 91 dias
    E eu tentar estornar o pagamento 0 com o motivo "Cobranca em duplicidade no cartao"
    Entao devo receber o erro "Prazo de 90 dias para estorno expirado"

  Cenario: Desistencia em ate 7 dias estorna tudo e cancela a matricula sem taxa
    Dado um aluno chamado "Caio Dantas" com CPF "72711111103"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "CARTAO"
    Quando se passarem 5 dias
    E eu estornar "" do pagamento 0 na categoria "DESISTENCIA" com o motivo "Aluno desistiu dentro do prazo legal"
    Entao o pagamento 0 deve estar estornado
    E o status da matricula deve ser "CANCELADA"
    E nao deve haver taxa de cancelamento

  Cenario: Desistencia apos 7 dias nao e aceita
    Dado um aluno chamado "Duda Esteves" com CPF "72711111104"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "CARTAO"
    Quando se passarem 8 dias
    E eu tentar estornar "" do pagamento 0 na categoria "DESISTENCIA" com o motivo "Aluno quer desistir fora do prazo"
    Entao devo receber o erro "Desistencia so pode ser estornada em ate 7 dias do pagamento"

  Cenario: Duplicidade exige outro pagamento de mesmo valor
    Dado um aluno chamado "Enzo Faria" com CPF "72711111105"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando eu tentar estornar "" do pagamento 0 na categoria "COBRANCA_DUPLICADA" com o motivo "Aluno diz que pagou duas vezes"
    Entao devo receber o erro "Nao ha outro pagamento de mesmo valor em ate 3 dias para caracterizar duplicidade"

  Cenario: Duplicidade confirmada estorna o pagamento repetido
    Dado um aluno chamado "Fabio Gama" com CPF "72711111106"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando eu estornar "" do pagamento 1 na categoria "COBRANCA_DUPLICADA" com o motivo "Mesmo PIX enviado duas vezes"
    Entao o pagamento 1 deve estar estornado
    E o total pago da matricula deve ser "R$ 120,00"

  # ---------- Contestacao: parcial, pedido de informacoes, recurso e prazos ----------

  Cenario: Contestacao parcialmente procedente estorna apenas parte do valor
    Dado um aluno chamado "Gabi Horta" com CPF "72711111107"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando o aluno contestar o pagamento 0 com o motivo "Cobraram R$ 21 a mais que o plano"
    E o analista "Marta" decidir a contestacao como "PARCIALMENTE_PROCEDENTE" estornando "21,00" com o parecer "Diferenca confirmada no sistema"
    Entao a contestacao deve estar "PARCIALMENTE_PROCEDENTE"
    E o total pago salvo da matricula deve ser "R$ 99,00"

  Cenario: Equipe pede informacoes e so pode decidir depois da resposta do aluno
    Dado um aluno chamado "Hugo Ilha" com CPF "72711111108"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "CARTAO"
    E o aluno contestar o pagamento 0 com o motivo "Nao reconheco esta cobranca no cartao"
    Quando a equipe pedir informacoes com a pergunta "Envie os 4 ultimos digitos do cartao cobrado"
    E o analista "Marta" tentar decidir a contestacao como "IMPROCEDENTE" com o parecer "Sem resposta ainda do aluno"
    Entao devo receber o erro "Contestacao aguardando resposta do aluno"
    Quando o aluno responder a contestacao com "Final 4321"
    Entao a contestacao deve estar "ABERTA"

  Cenario: Recurso e julgado por outro analista e pode reverter a decisao
    Dado um aluno chamado "Iris Jardim" com CPF "72711111109"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E o aluno contestar o pagamento 0 com o motivo "Fui cobrado duas vezes neste mes"
    E o analista "Marta" decidir a contestacao como "IMPROCEDENTE" com o parecer "Nao achei a segunda cobranca"
    Quando o aluno recorrer com a justificativa "Segue o comprovante das duas transferencias no extrato"
    E o analista "Marta" tentar decidir a contestacao como "PROCEDENTE" com o parecer "Revendo minha propria decisao"
    Entao devo receber o erro "O recurso deve ser julgado por outro analista"
    Quando o analista "Paulo" decidir a contestacao como "PROCEDENTE" com o parecer "Comprovante confirma a duplicidade"
    Entao a contestacao deve estar "PROCEDENTE"
    E o pagamento 0 deve estar estornado

  Cenario: Recurso so pode ser usado uma vez
    Dado um aluno chamado "Joao Kehl" com CPF "72711111110"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E o aluno contestar o pagamento 0 com o motivo "Fui cobrado duas vezes neste mes"
    E o analista "Marta" decidir a contestacao como "IMPROCEDENTE" com o parecer "Nao achei a segunda cobranca"
    E o aluno recorrer com a justificativa "Segue o comprovante das duas transferencias no extrato"
    E o analista "Paulo" decidir a contestacao como "IMPROCEDENTE" com o parecer "Comprovante e de outra academia"
    Quando o aluno tentar recorrer com a justificativa "Quero recorrer de novo desta decisao final"
    Entao devo receber o erro "Recurso ja utilizado nesta contestacao"

  Cenario: Pagamento com mais de 30 dias nao pode ser contestado
    Dado um aluno chamado "Kira Luz" com CPF "72711111111"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    Quando se passarem 31 dias
    E o aluno tentar contestar o pagamento 0 com o motivo "Lembrei agora desta cobranca"
    Entao devo receber o erro "Prazo de 30 dias para contestar este pagamento expirado"

  Cenario: Contestacao sem analise em 5 dias fica atrasada
    Dado um aluno chamado "Leo Mafra" com CPF "72711111112"
    E que o aluno ja foi matriculado no plano "MENSAL"
    E eu registrar um pagamento de "120,00" via "PIX"
    E o aluno contestar o pagamento 0 com o motivo "Fui cobrado duas vezes neste mes"
    Quando se passarem 6 dias
    Entao a contestacao deve estar atrasada
