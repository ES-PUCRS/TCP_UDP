A aplicação a ser desenvolvida deve ter os seguintes módulos:

• Cliente, composto por:
omódulo de interação com o usuário: responsável por interpretar os
comandos do usuário e exibir informações ao mesmo;
omódulo de comunicação com o servidor: responsável pela comunicação
com o servidor.

• Servidor: módulo gerenciador da aplicação, que serve como hub para conexões
dos diversos clientes, registro dos usuários na aplicação e gerenciamento da
mesma.

A aplicação deverá trabalhar com dois tipos de endereços: broadcast e unicast. As
mensagens do cliente para o servidor devem ser sempre em unicast. Já as mensagens
do servidor para os clientes devem ser em broadcast, com exceção do registro inicial,
que é respondido em unicast.

Em um primeiro momento, o cliente deve se registrar no servidor e o servidor deve
retornar a confirmação do registro (em unicast). De tempos em tempos (a cada 10
segundos) o cliente deve enviar mensagens de keepalive para o servidor, ocorrendo a
desvinculação dos clientes para os quais ocorre timeout (20 segundos) por falta de
keepalive.

Os usuários devem utilizar a interface do cliente e podem utilizar os comandos que
estiverem disponíveis para ele. Os usuários poderão enviar mensagens (individual ou em
grupo) para outros usuários que estejam no chat

A comunicação entre cliente e servidor deve ser feita pelas seguintes mensagens de
controle:
• registro de entrada e saída no servidor (unicast);
• keepalive do cliente para o servidor (unicast).

Quanto à comunicação entre cliente e servidor, deve se observar que:
• duas portas de comunicação devem ser criadas:
-- uma para a troca de mensagens de dados;
-- uma para a troca de mensagens de controle.
• a comunicação entre dois usuários é uma comunicação de 1 para 1, e deve ser
realizada através do servidor;
• a comunicação entre usuários (1 para n) de uma sala de chat deve ser enviada
em unicast para o servidor, que irá repassar aos clientes através de broadcast;
• o envio de informações de controle, do servidor para todos os clientes, deve ser
feito em broadcast;
• deve ser possível criar, no mínimo, duas salas de chats simultâneas.



-- uma para a troca de mensagens de dados;
-- uma para a troca de mensagens de controle.
?????????????????
