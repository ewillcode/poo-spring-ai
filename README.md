# Forca com Spring AI

Projeto de Programação Orientada a Objetos da Univille, desenvolvido por **Andrey Rebelatto, Eduardo Will e Erick Anderson**.

Sistema Web no qual o usuário escolhe um tema e uma palavra secreta, e o Gemini tenta descobri-la, uma letra por vez.

## Como funciona

Informe um tema e uma palavra de 2 a 30 letras, sem espaços, números ou símbolos. Acentos e cedilha são desconsiderados: `ação` vira `ACAO`.

Clique em **Pedir palpite da IA** para solicitar uma letra. O backend revela todas as ocorrências da letra e registra a jogada. A IA vence ao descobrir a palavra e perde no quinto erro. A cada erro, o boneco perde uma parte.

A IA recebe apenas o tema, a quantidade de letras, as letras tentadas e as posições reveladas. A palavra secreta permanece no backend até o encerramento da partida.

A tela exibe um registro em tempo real das chamadas, das respostas e do tempo de espera. Em falhas temporárias, são feitas até três novas tentativas, com intervalos progressivos. Os limites temporários da API recebem uma espera maior; erros de chave, permissão, modelo ou cota diária são informados sem insistir em novas chamadas. Falhas da API não consomem tentativas do jogo.

O histórico permite consultar partidas e continuar as que estiverem em andamento. Iniciar o programa não cria partidas ou jogadas automaticamente.

## Tecnologias

- Java 17 e Maven Wrapper.
- Spring Boot 4.1.2 e Spring MVC.
- Spring AI 2.0.1, com `ChatClient` e integração Google GenAI.
- Gemini, com modelo padrão `gemini-3.1-flash-lite`.
- Spring Data JPA e Hibernate.
- H2 em arquivo para persistência das partidas e jogadas.
- Thymeleaf, HTML, CSS, JavaScript e SVG.
- Server-Sent Events (SSE) para acompanhamento em tempo real.

O código está organizado nos pacotes `controller`, `service`, `model`, `repository`, `dto` e `config`.

## Como executar

É necessário ter **JDK 17 ou superior**, acesso à internet e uma chave da API Gemini criada no [Google AI Studio](https://aistudio.google.com/api-keys). O Maven Wrapper baixa o Maven e as dependências no primeiro uso.

1. Clone o repositório e entre na pasta do projeto.
2. Copie `.env.example` para `.env` na raiz.
3. Preencha `GEMINI_API_KEY` com sua chave.

```dotenv
GEMINI_API_KEY=sua_chave_aqui
GEMINI_MODEL=gemini-3.1-flash-lite
GEMINI_INTERVALO_MS=6500
```

O arquivo `.env` é carregado automaticamente. Use `CHAVE=valor`, sem `export` ou aspas. Se já houver outro modelo no seu `.env`, atualize `GEMINI_MODEL`. O intervalo entre chamadas pode ser ajustado conforme os limites do seu projeto no AI Studio.

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```sh
sh mvnw spring-boot:run
```

Acesse **http://localhost:8080**. A integração exige uma chave válida, acesso ao modelo e cota disponível. Após alterar o `.env`, reinicie a aplicação.

Para gerar e executar o JAR no Windows:

```powershell
.\mvnw.cmd package
java -jar target/poo-spring-ai-0.0.1-SNAPSHOT.jar
```

Execute os comandos a partir da raiz do projeto para carregar o `.env` e utilizar o mesmo banco. Os dados ficam em `data/forca.mv.db` e permanecem após reiniciar o programa. Para limpar todo o histórico, pare a aplicação e remova a pasta `data`.

O `.env`, o banco local e os arquivos de build são ignorados pelo Git. O `.env.example` contém somente os campos de configuração, sem credenciais.
