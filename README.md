# Estudo de Microsserviços com Java

Projeto de estudo prático de **microsserviços** utilizando **Java**, **Spring Boot**, **RabbitMQ** e **PostgreSQL**, com comunicação **assíncrona** entre serviços.

Quando um usuário é cadastrado no serviço `ms-user`, uma mensagem é publicada em uma fila do RabbitMQ. O serviço `ms-email` consome essa mensagem, envia um e-mail de boas-vindas (via SMTP do Gmail) e persiste o registro do envio no banco de dados.

## Arquitetura

```mermaid
flowchart LR
    Client[Cliente HTTP] -->|POST /users| User[ms-user :8080]

    subgraph RabbitMQ
        Queue[Fila default.email]
    end

    User -->|publica mensagem| Queue
    Queue -->|consome mensagem| Email[ms-email :8081]

    subgraph Infra
        PgUser[(PostgreSQL user-db :5432)]
        PgEmail[(PostgreSQL email-db :5433)]
        SMTP[SMTP Gmail]
    end

    User --> PgUser
    Email --> PgEmail
    Email -->|envia e-mail de boas-vindas| SMTP
```

### Fluxo

1. O cliente chama `POST /users` no `ms-user`;
2. O usuário é salvo no banco `user-db` e uma mensagem é publicada na fila `default.email` do RabbitMQ;
3. O `ms-email` consome a mensagem da fila;
4. O serviço envia o e-mail de boas-vindas e salva o registro na tabela `TB_EMAILS` com status `SENT` ou `ERROR`.

## Serviços

| Serviço | Porta | Responsabilidade |
| --- | --- | --- |
| `ms-user` | 8080 | API REST de cadastro de usuários. Salva o usuário e publica mensagem no RabbitMQ |
| `ms-email` | 8081 | Consome a fila, envia e-mails via SMTP e registra o status do envio |

## Stack

- Java 21
- Spring Boot 4.1.1 (WebMVC, Data JPA, AMQP, Mail, Security, Validation)
- RabbitMQ (mensageria / filas)
- PostgreSQL (persistência, um banco por serviço)
- Maven (build)
- Docker & Docker Compose (execução da infraestrutura)

## Estrutura do Projeto

```
microservices/
├── docker-compose.yaml       # Orquestra todos os serviços e dependências
├── .env                      # Credenciais do e-mail (não versionado)
├── ms-user/                  # Microsserviço de usuários
│   └── src/main/java/com/ms/user/
│       ├── configs/          # Configuração do RabbitMQ (conversor JSON)
│       ├── controllers/      # UserController (POST /users)
│       ├── dtos/             # UserRecordDto, EmailDto
│       ├── models/           # Entidade User (TB_USERS)
│       ├── producers/        # UserProducer (publica na fila)
│       ├── repositories/     # UserRepository
│       └── services/         # UserService (salva e publica)
└── ms-email/                 # Microsserviço de e-mail
    └── src/main/java/com/ms/msemail/
        ├── configs/          # Configuração da fila e do RabbitMQ
        ├── consumers/        # EmailConsumer (@RabbitListener)
        ├── dtos/             # EmailRecordDto
        ├── enums/            # StatusEmail (SENT, ERROR)
        ├── models/           # Entidade Email (TB_EMAILS)
        ├── repositories/     # EmailRepository
        └── services/         # EmailService (envio via JavaMail)
```

## Como executar

### Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven (para executar os serviços fora do Docker)
- Uma conta Gmail com **senha de app** para envio dos e-mails

### 1. Configurar credenciais de e-mail

Crie o arquivo `.env` na raiz do projeto (já ignorado pelo git):

```env
MAIL_USERNAME=seu.email@gmail.com
MAIL_PASSWORD=sua-senha-de-app
```

> O `ms-email` envia e-mails pelo SMTP do Gmail. Caso o Gmail bloqueie o login,
> gere uma [senha de app](https://support.google.com/accounts/answer/185833) e a utilize no `MAIL_PASSWORD`.

### 2. Subir a infraestrutura

```bash
docker compose up --build
```

Serviços disponíveis:

| Recurso | Endereço |
| --- | --- |
| API `ms-user` | http://localhost:8080 |
| API `ms-email` | http://localhost:8081 |
| RabbitMQ Management | http://localhost:15672 (usuário/senha: `admin` / `12345`) |
| PostgreSQL `user-db` | localhost:5432 |
| PostgreSQL `email-db` | localhost:5433 |

### 3. Testando o fluxo

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria da Silva","email":"maria@example.com"}'
```

Resposta esperada (HTTP `201 Created`):

```json
{
  "userId": "29f0f8f4-8c0a-4f8e-8f0a-...",
  "name": "Maria da Silva",
  "email": "maria@example.com",
  "password": null
}
```

Após a resposta, acompanhe no RabbitMQ Management a fila `default.email` e o
recebimento do e-mail de boas-vindas no endereço informado.

## Notas de estudo

- Comunicação assíncrona entre serviços com RabbitMQ (padrão *event-driven*);
- Isolamento de dados: cada microsserviço possui seu próprio banco PostgreSQL;
- Modelo *Transactional Outbox* parcial: o usuário é salvo e a mensagem publicada na mesma transação de serviço;
- Dockerfile em multi-estágio para reduzir o tamanho das imagens;
- Configuração da fila `default.email` definida via `broker.queue.email.name` no `application.properties` de cada serviço.
