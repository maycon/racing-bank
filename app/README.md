# 🏦 Hack N Roll Racing Bank - Android App

![Version](https://img.shields.io/badge/version-1.0.0-cyan)
![Android](https://img.shields.io/badge/Android-API%2024+-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple)

## 🎮 Sobre o Projeto

**Hack N Roll Race Bank** é uma aplicação Android com visual retrô cyberpunk que consome uma API bancária educacional com vulnerabilidades intencionais de race conditions. O app oferece uma experiência bancária completa com um design nostálgico dos anos 80/90.

### ⚠️ AVISO IMPORTANTE
Esta aplicação foi desenvolvida para fins educacionais e demonstração de conceitos. A API possui vulnerabilidades intencionais e **NÃO DEVE SER USADA EM PRODUÇÃO**.

## 🎨 Features

### 💰 Funcionalidades Bancárias
- **Autenticação Segura**: Login com 2FA (TOTP)
- **Dashboard Interativo**: Visualização de saldo e investimentos
- **Depósitos e Saques**: Operações em conta corrente
- **Transferências**: Envio de dinheiro entre contas
- **Investimentos**: Compra e venda de cotas do fundo
- **Extrato**: Histórico completo de transações

### 🎪 Visual Retrô
- **Tema Cyberpunk**: Cores neon vibrantes (cyan, pink, purple)
- **Animações Fluidas**: Transições suaves entre telas
- **Efeitos Sonoros**: Sons retrô de 8-bit (opcional)
- **Fontes Pixeladas**: Typography estilo terminal

## 📱 Screenshots

```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   SPLASH SCREEN │  │     LOGIN       │  │    DASHBOARD    │
│                 │  │                 │  │                 │
│  HACK N ROLL    │  │  ┌───────────┐  │  │ Cash: $1,000    │
│   RACE BANK     │  │  │ Username  │  │  │ Fund: $500      │
│                 │  │  └───────────┘  │  │                 │
│   LOADING...    │  │  ┌───────────┐  │  │ [DEPOSIT]       │
│                 │  │  │ Password  │  │  │ [WITHDRAW]      │
│                 │  │  └───────────┘  │  │ [TRANSFER]      │
│                 │  │                 │  │ [INVEST]        │
│                 │  │   [ LOGIN ]     │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## 🛠️ Tecnologias Utilizadas

### Core
- **Kotlin** - Linguagem principal
- **Android SDK** - Min API 24, Target API 34
- **AndroidX** - Bibliotecas de suporte modernas

### Arquitetura
- **MVVM** - Model-View-ViewModel
- **Repository Pattern** - Abstração de dados
- **LiveData & Flow** - Programação reativa
- **Coroutines** - Operações assíncronas

### Networking
- **Retrofit 2** - Cliente HTTP
- **OkHttp 3** - Interceptors e logging
- **Gson** - Serialização JSON

### UI/UX
- **Material Design 3** - Componentes modernos
- **View Binding** - Type-safe view references
- **Navigation Component** - Navegação entre fragments
- **Lottie** - Animações complexas

### Segurança
- **EncryptedSharedPreferences** - Armazenamento seguro
- **TOTP** - Autenticação de dois fatores
- **JWT** - Tokens de autenticação

## 📦 Estrutura do Projeto

```
app/
├── src/main/java/com/hacknroll/bank/
│   ├── data/
│   │   ├── api/          # Retrofit service e cliente
│   │   ├── models/       # Data classes
│   │   └── repository/   # Repository pattern
│   ├── ui/
│   │   ├── auth/         # Login, Register, 2FA
│   │   ├── main/         # MainActivity e fragments
│   │   │   └── fragments/
│   │   │       ├── DashboardFragment
│   │   │       ├── TransferFragment
│   │   │       ├── InvestmentFragment
│   │   │       ├── StatementFragment
│   │   │       └── SettingsFragment
│   │   └── splash/       # Splash screen
│   └── utils/           # Utilities e helpers
├── src/main/res/
│   ├── layout/          # XML layouts
│   ├── values/          # Colors, strings, themes
│   ├── drawable/        # Ícones e backgrounds
│   ├── anim/           # Animações
│   └── font/           # Fontes customizadas
└── build.gradle.kts    # Configurações do build
```

## 🚀 Como Executar

### Pré-requisitos
- Android Studio Arctic Fox ou superior
- JDK 17
- Android SDK instalado
- Emulador ou dispositivo físico (API 24+)

### Instalação

1. **Clone o repositório**
```bash
git clone https://github.com/maycon/racing-bank.git
cd racing-bank
```

2. **Configure a API**
- Certifique-se de que a API está rodando localmente na porta 8000
- Para emulador: URL já configurada como `http://10.0.2.2:8000`
- Para dispositivo físico: Altere em `RetrofitClient.kt` para o IP da sua máquina

3. **Abra no Android Studio**
- File → Open → Selecione a pasta do projeto
- Aguarde a sincronização do Gradle

4. **Execute o app**
- Selecione um dispositivo/emulador
- Clique em Run (▶️)

## 🎯 Fluxo de Uso

### 1. Registro
- Abra o app e vá para a aba "REGISTER"
- Crie uma conta com username e senha
- Salve o TOTP secret fornecido

### 2. Configuração 2FA
- Use Google Authenticator ou similar
- Escaneie o QR code ou insira o secret manualmente

### 3. Login
- Entre com username e senha
- Insira o código de 6 dígitos do autenticador

### 4. Operações Bancárias
- **Depósito**: Adicione fundos à conta
- **Saque**: Retire fundos
- **Transferência**: Envie para outro usuário
- **Investimento**: Compre cotas do fundo
- **Resgate**: Venda cotas do fundo

## 🐛 Race Conditions Educacionais

A API possui vulnerabilidades intencionais para demonstração:

### Double Spending em Transferências
```kotlin
// Duas transferências simultâneas podem ultrapassar o saldo
coroutineScope {
    launch { transfer("user1", 100.0) }
    launch { transfer("user2", 100.0) }
}
```

### Lost Updates em Depósitos
```kotlin
// Depósitos concorrentes podem perder atualizações
repeat(10) {
    GlobalScope.launch {
        deposit(50.0)
    }
}
```

### Inconsistência em Investimentos
```kotlin
// Operações simultâneas no fundo podem gerar inconsistências
coroutineScope {
    launch { subscribeToFund(1000.0) }
    launch { redeemFromFund(500.0) }
}
```

## 🎨 Customização

### Temas
O app suporta três temas:
- **Retro Cyberpunk** (padrão)
- **Light Mode**
- **System Default**

### Cores Principais
```xml
<color name="retro_cyan">#00FFFF</color>
<color name="retro_pink">#FF1493</color>
<color name="retro_purple">#9400D3</color>
<color name="retro_green">#00FF00</color>
<color name="retro_yellow">#FFD700</color>
```

### Fontes
Para adicionar fontes retrô:
1. Coloque arquivos .ttf em `res/font/`
2. Reference em `themes.xml`

## 📝 TODO List

- [ ] Implementar biometria real
- [ ] Adicionar gráficos de investimento
- [ ] Melhorar animações
- [ ] Adicionar mais efeitos sonoros
- [ ] Implementar notificações push
- [ ] Cache offline com Room
- [ ] Testes unitários e instrumentados
- [ ] CI/CD com GitHub Actions

## 🔒 Segurança

### Boas Práticas Implementadas
- ✅ Armazenamento criptografado de tokens
- ✅ Autenticação de dois fatores
- ✅ Validação de inputs
- ✅ HTTPS em produção
- ✅ Ofuscação com ProGuard

### ⚠️ Vulnerabilidades Educacionais
- ❌ Race conditions na API (intencional)
- ❌ Sem rate limiting (educacional)
- ❌ Debug logging habilitado

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto é apenas para fins educacionais. Não use em produção.

## 👥 Autores

- **Maycon Vitali** - *Desenvolvimento* - [GitHub](https://github.com/maycon)

## 🙏 Agradecimentos

- Criado como DEMO para a aplicação [TREM](https://github.com/otavioarj/TREM)
- API de demonstração com vulnerabilidades educacionais
- Comunidade Android por bibliotecas open source

**⚡ Hack N Roll Racing Bank** - *Banking with a retro twist!* 🎮💰
