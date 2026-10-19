"""Gera o relatório de vulnerabilidades em Markdown a partir dos resultados JSON do Trivy.

Uso: python relatorio_trivy.py <repositorio.json> <imagem.json> <saida.md>
"""
import json
import os
import sys
from datetime import datetime, timezone

SEVERIDADES = ["CRITICAL", "HIGH", "MEDIUM"]


def carregar(caminho):
    try:
        with open(caminho, encoding="utf-8") as arquivo:
            return json.load(arquivo)
    except (OSError, ValueError):
        return {}


def texto(valor):
    return str(valor or "—").replace("|", "\\|").replace("\n", " ")


def achados(resultado):
    """Retorna (tipo, item) de cada vulnerabilidade, configuração incorreta e segredo do alvo."""
    for item in resultado.get("Vulnerabilities") or []:
        yield "vulnerabilidade", item
    for item in resultado.get("Misconfigurations") or []:
        if item.get("Status", "FAIL") == "FAIL":
            yield "configuracao", item
    for item in resultado.get("Secrets") or []:
        yield "segredo", item


def contagem(relatorio):
    linhas = []
    for resultado in relatorio.get("Results") or []:
        totais = {s: 0 for s in SEVERIDADES}
        for _, item in achados(resultado):
            if item.get("Severity") in totais:
                totais[item["Severity"]] += 1
        linhas.append((resultado.get("Target", "—"), totais))
    return linhas


def secao(titulo, descricao, relatorio):
    md = [f"## {titulo}", "", descricao, ""]
    resultados = relatorio.get("Results") or []
    if not relatorio:
        return md + ["> ⚠️ Resultado não encontrado: a varredura não gerou o arquivo JSON.", ""]

    alvos = ", ".join(f"`{r.get('Target')}`" for r in resultados) or "—"
    md += [f"**Alvos analisados:** {alvos}", ""]

    vulns, configs, segredos = [], [], []
    for resultado in resultados:
        alvo = resultado.get("Target", "—")
        for tipo, item in achados(resultado):
            {"vulnerabilidade": vulns, "configuracao": configs, "segredo": segredos}[tipo].append((alvo, item))

    if not (vulns or configs or segredos):
        return md + ["✅ **Nenhuma vulnerabilidade, segredo ou configuração incorreta** de severidade "
                     f"{', '.join(SEVERIDADES)} encontrada.", ""]

    ordem = {s: i for i, s in enumerate(SEVERIDADES)}
    if vulns:
        md += ["### Vulnerabilidades", "",
               "| Severidade | CVE | Pacote | Versão instalada | Versão corrigida | Título | Alvo |",
               "|---|---|---|---|---|---|---|"]
        for alvo, v in sorted(vulns, key=lambda x: ordem.get(x[1].get("Severity"), 99)):
            cve = v.get("VulnerabilityID")
            link = f"[{cve}]({v['PrimaryURL']})" if v.get("PrimaryURL") else texto(cve)
            md.append(f"| {texto(v.get('Severity'))} | {link} | `{texto(v.get('PkgName'))}` | "
                      f"{texto(v.get('InstalledVersion'))} | {texto(v.get('FixedVersion'))} | "
                      f"{texto(v.get('Title'))} | `{texto(alvo)}` |")
        md.append("")
    if configs:
        md += ["### Configurações incorretas", "",
               "| Severidade | Regra | Título | Mensagem | Alvo |", "|---|---|---|---|---|"]
        for alvo, c in sorted(configs, key=lambda x: ordem.get(x[1].get("Severity"), 99)):
            regra = c.get("AVDID") or c.get("ID")
            link = f"[{regra}]({c['PrimaryURL']})" if c.get("PrimaryURL") else texto(regra)
            md.append(f"| {texto(c.get('Severity'))} | {link} | {texto(c.get('Title'))} | "
                      f"{texto(c.get('Message'))} | `{texto(alvo)}` |")
        md.append("")
    if segredos:
        md += ["### Segredos", "", "| Severidade | Regra | Título | Linha | Alvo |", "|---|---|---|---|---|"]
        for alvo, s in segredos:
            md.append(f"| {texto(s.get('Severity'))} | {texto(s.get('RuleID'))} | {texto(s.get('Title'))} | "
                      f"{texto(s.get('StartLine'))} | `{texto(alvo)}` |")
        md.append("")
    return md


def main():
    repositorio, imagem, saida = carregar(sys.argv[1]), carregar(sys.argv[2]), sys.argv[3]

    versao = (repositorio.get("Trivy") or imagem.get("Trivy") or {}).get("Version", "—")
    servidor = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
    repo = os.environ.get("GITHUB_REPOSITORY", "")
    sha = os.environ.get("GITHUB_SHA", "")
    execucao = os.environ.get("GITHUB_RUN_ID", "")
    commit = f"[`{sha[:7]}`]({servidor}/{repo}/commit/{sha})" if sha else "—"
    link_execucao = f"[#{execucao}]({servidor}/{repo}/actions/runs/{execucao})" if execucao else "—"

    md = [
        "# Relatório de vulnerabilidades",
        "",
        f"- **Data da análise:** {datetime.now(timezone.utc).strftime('%d/%m/%Y %H:%M')} (UTC)",
        f"- **Ferramenta:** [Trivy](https://trivy.dev) {versao}",
        f"- **Branch / commit:** `{os.environ.get('GITHUB_REF_NAME', '—')}` / {commit}",
        f"- **Execução:** {link_execucao}",
        f"- **Severidades consideradas:** {', '.join(SEVERIDADES)}",
        "",
        "## Resumo",
        "",
        "| Alvo | CRITICAL | HIGH | MEDIUM | Total |",
        "|---|---|---|---|---|",
    ]
    for nome, relatorio in (("Repositório", repositorio), ("Imagem Docker", imagem)):
        for alvo, totais in contagem(relatorio):
            md.append(f"| {nome}: `{texto(alvo)}` | " + " | ".join(str(totais[s]) for s in SEVERIDADES)
                      + f" | {sum(totais.values())} |")
    md.append("")

    md += secao("Repositório (dependências, segredos e configuração)",
                "Varredura do código-fonte: dependências do `pom.xml`, segredos e configuração do `Dockerfile`.",
                repositorio)
    md += secao("Imagem Docker",
                "Varredura da imagem da aplicação: pacotes do sistema (Alpine) e bibliotecas do `app.jar`.",
                imagem)
    md += [
        "## Análise de aplicabilidade",
        "",
        "A avaliação das vulnerabilidades remanescentes (se são exploráveis nesta aplicação), as correções aplicadas "
        "e as decisões de risco estão documentadas em `docs/seguranca/relatorio-vulnerabilidades.md`.",
        "",
    ]

    with open(saida, "w", encoding="utf-8") as arquivo:
        arquivo.write("\n".join(md))


if __name__ == "__main__":
    main()
