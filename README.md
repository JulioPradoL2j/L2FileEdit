# L2FileEdit (Samurai Edition)

Editor de arquivos do client Lineage II (System) com foco em **Samurai / Essence**, com interface moderna (dark) e suporte a **decrypt/encrypt de .dat**, além de ações em massa (unpack/pack/patch).

> Projeto em Java (Swing). Licença: **GPL-3.0**.

---

## ✨ Recursos

- **Abrir e editar** arquivos:
  - `.dat` (com decrypt automático quando necessário)
  - `.ini`, `.txt`, `.htm`
- **Salvar**
  - `Save TXT` (exportar para texto)
  - `Save & Encrypt` (salvar .dat recriptografado)
- **Ações em massa**
  - `Unpack all (folder)` — decriptar/exportar em lote
  - `Pack all (folder)` — recriptar/empacotar em lote
  - `Patch all (folder)` — recrypt/patch em lote
- **Dump ServerName Bytes**
  - ferramenta auxiliar para gerar dump do `ServerName-eu.dat`
- **Editor melhorado**
  - números de linha
  - undo/redo
  - menu de contexto (copy/cut/paste/delete)
  - Go to line (Ctrl+G)
  - Find/Find Next (Ctrl+F / F3)

---

## 🧠 Como funciona o modo **Source**
Quando o Encrypt está como `Source`, o editor tenta **salvar o .dat usando a mesma versão** detectada na hora que você abriu/decriptou o arquivo (ex.: `Lineage2Ver###`).

- ✅ Abriu com VerXYZ → salva com VerXYZ
- ⚠️ Se o arquivo não estava criptografado, o “Source” pode não ter uma versão detectada para reaproveitar (recomendado definir um fallback no código para Samurai).

---

## 🖼️ Screenshots

> Coloque suas imagens em `docs/img/` e descomente:

<!--
![Main UI](docs/img/main-ui.png)
![Open Dialog](docs/img/open-dialog.png)
-->

---

## 📦 Download / Build

Este repositório contém estrutura típica de projeto Java/Swing e pode ser compilado via IDE ou build script.

### Requisitos
- Java (recomendado 25+)
- (Opcional) Eclipse/IntelliJ

### Rodando pela IDE (mais fácil)
1. Importe o projeto
2. Execute:
   - `net.sf.l2jdev.L2FileEdit`

### Build
- Se você usa `build.xml` (Ant), rode:
  - `ant` / `ant dist` (depende do seu alvo no `build.xml`)

> Se você publicar “Releases”, recomendo anexar o `.zip` com `dist/` e instruções do `launcher`.

---

## 📁 Estrutura importante

- `java/net/sf/l2jdev/` — código-fonte
- `data/` — estruturas/descritores usados para parse dos `.dat` (dependendo do seu projeto)
- `images/` ou `src/main/resources/images/` — ícones do app (recomendado usar resources no classpath)
- `config/` — configurações (ex.: log)

---

## 🧩 Suporte de formatos

- `.ini` / `.txt`: lido como UTF-8
- `.htm`: lido como UTF-16 (padrão comum em alguns system/HTML de L2)
- `.dat`: decrypt/encrypt usando as chaves/versões configuradas no projeto

> Observação: estruturas/versões disponíveis dependem dos descritores presentes no projeto.

---

## 🛠️ Customização (UI / Tema)
A UI foi ajustada para um layout moderno:
- sidebar com ações (File / Tools / Debug)
- editor principal com line numbers
- painel de logs em abas (Log / Error / Program)
- tema dark com overrides no Nimbus

---

## 🤝 Contribuição

Pull Requests são bem-vindos, especialmente para:
- novos descritores/estruturas do Samurai
- melhorias no parser
- melhorias de UI/UX (atalhos, tabs, status bar)
- correções de encode/compatibilidade

---

## 📄 Licença

Este projeto é distribuído sob **GPL-3.0**.  
Leia o arquivo `LICENSE` para detalhes.

---

## ✅ Créditos

- Comunidade L2J / desenvolvimento open-source de ferramentas e estruturas para edição de client files.
- Autores e contribuidores deste repositório.

---

## English (optional)

**L2FileEdit (Samurai Edition)** is a Swing-based editor for Lineage II client System files with auto decrypt/encrypt for `.dat`, batch operations, and a modern dark UI.