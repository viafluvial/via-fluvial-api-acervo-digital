# Runbook de Operacao do Acervo Digital (PRD)

Este documento consolida o processo operacional executado para provisionar, validar e publicar a CDN do Acervo Digital em producao.

## 1) Contexto operacional atual

- Repositorio: viafluvial-via-fluvial-gcp-acervo-digital
- Ambiente: prd
- Projeto de deploy do acervo: vfa-plataforma-prd-k0ru
- Regiao do ambiente: us-central1
- Bucket remoto de state da foundation: via-fluvial-foundation-terraform-state-prd
- Prefixo de state para este modulo: acervo-digital-prd
- Projeto de DNS: vfa-dns-core
- Zona DNS: zona-dns-viafluvial-com-br
- Dominio da CDN: cdn.viafluvial.com.br

## 2) Pre-requisitos

- Terraform instalado e funcional no host.
- gcloud CLI instalado e autenticacao via service account.
- Credencial local em credentials/sa.json.
- Permissao para:
  - Ler e gravar state no bucket remoto da foundation.
  - Criar e atualizar recursos no projeto vfa-plataforma-prd-k0ru.
  - Alterar DNS no projeto vfa-dns-core.

## 3) Autenticacao (ADC)

Sempre exportar a credencial antes dos comandos Terraform ou gcloud:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
```

Se aparecer erro de credenciais, validar:

```bash
test -f credentials/sa.json && echo "ok"
```

## 4) Inicializacao de PRD com backend remoto

Comando recomendado:

```bash
make init ENV=prd BACKEND_BUCKET=via-fluvial-foundation-terraform-state-prd BACKEND_PREFIX=acervo-digital-prd
```

Observacao:
- BACKEND_BUCKET e o bucket de state remoto.
- BACKEND_PREFIX separa o state deste modulo dentro do bucket.

## 5) Ciclo de deploy em PRD

Validar configuracao:

```bash
make validate ENV=prd
```

Gerar plano:

```bash
make plan ENV=prd
```

Aplicar plano:

```bash
make apply ENV=prd
```

Ler outputs:

```bash
make output ENV=prd
```

## 6) Outputs criticos para operacao

Comandos uteis:

```bash
terraform -chdir=envs/prd output
terraform -chdir=envs/prd output -raw cdn_ip_address
```

Saidas importantes:
- cdn_ip_address
- cdn_base_url
- public_media_bucket_name
- quarantine_bucket_name
- private_documents_bucket_name
- acervo_api_service_account_email
- acervo_admin_service_account_email

## 7) Preenchimento de env por ambiente

Arquivo de PRD:
- envs/prd/.env.example

Campos devem refletir os outputs do Terraform do ambiente PRD.

## 8) Publicacao de DNS da CDN

### 8.1 Descobrir zona

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
DNS_PROJECT="vfa-dns-core"
gcloud dns managed-zones list --project="$DNS_PROJECT"
```

### 8.2 Criar ou atualizar A record para o IP da CDN

Exemplo de automacao usada:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
DNS_PROJECT="vfa-dns-core"
FQDN="cdn.viafluvial.com.br."
NEW_IP="34.49.97.234"

ZONE=$(gcloud dns managed-zones list \
  --project="$DNS_PROJECT" \
  --filter="dnsName:viafluvial.com.br." \
  --format="value(name)" | head -n1)

CURRENT_IPS=$(gcloud dns record-sets list \
  --project="$DNS_PROJECT" \
  --zone="$ZONE" \
  --name="$FQDN" \
  --type=A \
  --format="value(rrdatas[])" | xargs)

gcloud dns record-sets transaction start --project="$DNS_PROJECT" --zone="$ZONE"

if [ -n "$CURRENT_IPS" ]; then
  gcloud dns record-sets transaction remove $CURRENT_IPS \
    --project="$DNS_PROJECT" \
    --zone="$ZONE" \
    --name="$FQDN" \
    --ttl=300 \
    --type=A
fi

gcloud dns record-sets transaction add "$NEW_IP" \
  --project="$DNS_PROJECT" \
  --zone="$ZONE" \
  --name="$FQDN" \
  --ttl=300 \
  --type=A

gcloud dns record-sets transaction execute --project="$DNS_PROJECT" --zone="$ZONE"
```

## 9) Validacao pos-DNS

### 9.1 Resolver DNS

```bash
dig +short cdn.viafluvial.com.br
nslookup cdn.viafluvial.com.br
```

Esperado:
- Retornar o IP da CDN (exemplo atual: 34.49.97.234).

### 9.2 Validar HTTP e HTTPS

```bash
curl -I http://cdn.viafluvial.com.br
curl -I https://cdn.viafluvial.com.br
```

Leitura correta dos resultados:
- HTTP 403 pode ser esperado sem objeto publico especifico. Isso prova que DNS e roteamento estao corretos.
- HTTPS pode falhar nos primeiros minutos enquanto o certificado gerenciado ainda estiver em provisionamento.

## 10) Status de certificado gerenciado

Comando:

```bash
gcloud compute ssl-certificates list \
  --project=vfa-plataforma-prd-k0ru \
  --format="table(name,type,managed.status,managed.domainStatus)"
```

Estados:
- PROVISIONING: ainda emitindo certificado.
- ACTIVE: pronto para HTTPS estavel.

## 11) Troubleshooting rapido

### Erro: could not find default credentials

Causa:
- ADC nao exportado na sessao.

Acao:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
```

### Erro: NXDOMAIN para cdn.viafluvial.com.br

Causa:
- Registro DNS nao criado ou nao propagado.

Acao:
- Criar/validar A record apontando para cdn_ip_address.

### Erro curl 6 (Could not resolve host)

Causa:
- Mesmo problema de DNS (sem resolucao).

Acao:
- Validar com dig e nslookup.

### Erro curl 35 em HTTPS

Causa comum:
- Certificado gerenciado ainda em PROVISIONING.

Acao:
- Aguardar propagacao DNS e emissao do certificado.
- Revalidar status do certificado no projeto PRD.

## 12) Regras de seguranca e governanca aplicadas

- Nao criar/alterar foundation, VPC, Shared VPC, folders, org e projetos fora do escopo.
- Nao alterar DNS automaticamente via Terraform deste modulo.
- Nao executar destroy no fluxo padrao do repositorio (alvo bloqueado no Makefile).
- Nao commitar credenciais ou segredos.

## 13) Comandos de referencia (sequencia completa)

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
make init ENV=prd BACKEND_BUCKET=via-fluvial-foundation-terraform-state-prd BACKEND_PREFIX=acervo-digital-prd
make validate ENV=prd
make plan ENV=prd
make apply ENV=prd
terraform -chdir=envs/prd output -raw cdn_ip_address
```

Depois do apply, atualizar DNS e validar acesso da CDN conforme secoes 8 a 10.

## 14) Matriz de testes recomendada (PRD)

Esta secao organiza os testes por camada para confirmar que a stack esta funcional e segura.

### 14.1 Testes de infraestrutura (Terraform e estado)

Objetivo:
- Garantir que nao ha erro de sintaxe, drift inesperado e outputs coerentes.

Comandos:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="$PWD/credentials/sa.json"
make validate ENV=prd
make plan ENV=prd
terraform -chdir=envs/prd state list
terraform -chdir=envs/prd output
```

Resultado esperado:
- validate sem erros.
- plan sem alteracoes inesperadas.
- state contendo buckets, IAM e CDN.

### 14.2 Testes de APIs habilitadas no projeto

Objetivo:
- Confirmar que apenas APIs necessarias ao modulo estao ativas.

Comando:

```bash
gcloud services list --enabled --project vfa-plataforma-prd-k0ru
```

Checar presenca de:
- storage.googleapis.com
- logging.googleapis.com
- monitoring.googleapis.com
- compute.googleapis.com

### 14.3 Testes de DNS e entrega CDN

Objetivo:
- Confirmar resolucao de nome e reachability da borda.

Comandos:

```bash
dig +short cdn.viafluvial.com.br
nslookup cdn.viafluvial.com.br
curl -I http://cdn.viafluvial.com.br
curl -I https://cdn.viafluvial.com.br
```

Resultado esperado:
- DNS resolvendo para o IP da CDN.
- HTTP com resposta da borda (403 pode ser aceitavel sem objeto publico valido).
- HTTPS respondendo apos certificado ficar ACTIVE.

### 14.4 Testes de certificado gerenciado

Objetivo:
- Confirmar emissao e ativacao de TLS para o dominio da CDN.

Comando:

```bash
gcloud compute ssl-certificates list \
  --project=vfa-plataforma-prd-k0ru \
  --format="table(name,type,managed.status,managed.domainStatus)"
```

Resultado esperado:
- managed.status = ACTIVE
- domainStatus para cdn.viafluvial.com.br = ACTIVE

### 14.5 Testes de buckets e controles de seguranca

Objetivo:
- Validar que buckets privados nao foram expostos e controles estao ativos.

Comandos:

```bash
gcloud storage buckets describe gs://viafluvial-acervo-digital-quarantine-prd \
  --format="value(iamConfiguration.uniformBucketLevelAccess.enabled,iamConfiguration.publicAccessPrevention)"

gcloud storage buckets describe gs://viafluvial-acervo-digital-private-documents-prd \
  --format="value(iamConfiguration.uniformBucketLevelAccess.enabled,iamConfiguration.publicAccessPrevention)"

gcloud storage buckets describe gs://viafluvial-acervo-digital-public-media-prd \
  --format="value(iamConfiguration.uniformBucketLevelAccess.enabled,iamConfiguration.publicAccessPrevention)"
```

Resultado esperado:
- quarantine: uniform access true, public access prevention enforced
- private documents: uniform access true, public access prevention enforced
- public media: uniform access true, public access prevention inherited

### 14.6 Testes de IAM funcional (acesso minimo)

Objetivo:
- Verificar que as service accounts possuem apenas acessos previstos.

Comandos:

```bash
terraform -chdir=envs/prd output -raw acervo_api_service_account_email
terraform -chdir=envs/prd output -raw acervo_admin_service_account_email
```

Validacoes recomendadas:
- API SA grava na quarentena.
- API SA le public media conforme politica.
- API SA nao tem owner/editor.
- Admin SA possui papeis de administracao de objetos conforme definido.

### 14.7 Testes de fluxo funcional ponta a ponta

Objetivo:
- Validar o processo de negocio Upload -> Quarentena -> Aprovacao -> Publicacao -> CDN.

Sequencia:
1. Fazer upload de midia para quarentena.
2. Aprovar no backend.
3. Mover/copy para bucket publico.
4. Validar acesso publico via URL da CDN.
5. Testar reprova e descarte conforme regra operacional.
6. Validar acesso de documentos privados apenas por fluxo autenticado/URL assinada.

### 14.8 Testes de CORS em frontend real

Objetivo:
- Confirmar que origins liberadas no PRD funcionam e origins nao permitidas falham.

Sugestao:
- Testar requests do frontend oficial (viafluvial.com.br e www.viafluvial.com.br).
- Testar origin nao permitido e validar bloqueio pelo navegador.

### 14.9 Criterio de aceite operacional

A stack pode ser considerada OK quando:
1. Terraform validate e plan estao sem anomalias.
2. DNS resolve para o IP da CDN.
3. Certificado gerenciado esta ACTIVE.
4. Buckets privados nao estao publicos.
5. Fluxo ponta a ponta publica apenas midia aprovada via CDN.
6. Documentos privados nao sao expostos diretamente.
