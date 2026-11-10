# Alerta Contato V2 — Firebase grátis (Spark)

Android Java. **Sem Cloud Functions e sem FCM.** Usa apenas Authentication por e-mail/senha e Realtime Database. Projeto Firebase: `appqua-22`.

## Firebase

1. Firebase Console → Authentication → Método de login → habilitar **E-mail/senha**.
2. Firebase Console → Realtime Database → Regras → substituir pelas regras em `database.rules.json` → **Publicar**. As regras são essenciais: nunca usar `read:true, write:true`.
3. Realtime Database: `https://appqua-22-default-rtdb.firebaseio.com`.
4. **Não precisa do plano Blaze, de Cloud Functions, nem de chave secreta de servidor.** A versão foi projetada para funcionar dentro da cota gratuita do Firebase Spark; uso acima dos limites pode ser bloqueado até o próximo período.

## GitHub Actions → APK

1. Envie esta pasta completa ao GitHub, incluindo `.github/workflows/android.yml`.
2. Actions → **Gerar APK Android** → Run workflow.
3. Baixe o artifact **AlertaContato-APK** e instale `app-debug.apk`.
4. O workflow aceita `FIREBASE_ANDROID_APP_ID` em GitHub Actions Secrets, mas para uma integração Android correta cadastre também o app Android `com.diemdev.alertacontato` no projeto Firebase e insira esse App ID (`1:122122220091:android:...`). O app usa configuração explícita, sem necessidade de `google-services.json` para esse conjunto de SDKs.

## Funcionamento

- Cada cadastro no Authentication gera ID público AC-XXXXXXXX no Realtime Database.
- Solicitação por ID grava `/requests/{destino}/{origem}`.
- Aceitar muda status para `accepted` e grava o pareamento em `/contacts/uidA/uidB` e `/contacts/uidB/uidA`, sujeito às regras de segurança.
- Ao manter o botão 3 segundos e soltar, inicia contagem de 3 segundos; ao fim publica `/status/{uid}` com `state: SOS`, `eventId` e timestamp.
- Enquanto o app receptor estiver **aberto/conectado e observando o Firebase**, a alteração em status gera uma notificação com sirene e vibração. O listener ignora o estado inicial já existente quando é conectado.
- Perfil armazena as informações de saúde no Realtime Database. Contatos pareados podem ler o perfil: compartilhe com cuidado.

### Limitações importantes

**Realtime Database não é push:** se o aplicativo estiver encerrado, sem rede, suspenso pelo Android ou impedido de executar em segundo plano, o SOS **não vai necessariamente tocar na hora**. Esta V2 não inclui serviço persistente de segundo plano nem fila de alertas não lidos; a solução é adequada para protótipo com ambos os apps abertos, **não** para proteção pessoal crítica. Som e volume dependem das permissões/canais do Android e do modo Não Perturbe. Não é sistema de alerta severo governamental.

O banco não substitui serviços 190 / 192 / 193. Antes de uso real, implemente controle de bloqueio, remoção de contatos, privacidade/LGPD, proteção antispam, recibo de leitura e testes em vários aparelhos.
