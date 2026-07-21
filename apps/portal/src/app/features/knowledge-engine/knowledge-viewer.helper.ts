export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export function renderMarkdownPreview(markdown: string): string {
  if (!markdown.trim()) {
    return '';
  }

  const placeholders: string[] = [];
  let working = markdown.replace(/```([\s\S]*?)```/g, (_match, code: string) => {
    const token = `@@CODEBLOCK_${placeholders.length}@@`;
    placeholders.push(`<pre class="knowledge-engine__code-block"><code>${escapeHtml(code.trim())}</code></pre>`);
    return token;
  });

  working = escapeHtml(working);
  working = working.replace(/`([^`\n]+)`/g, '<code class="knowledge-engine__inline-code">$1</code>');
  working = working.replace(/^###### (.+)$/gm, '<h6>$1</h6>');
  working = working.replace(/^##### (.+)$/gm, '<h5>$1</h5>');
  working = working.replace(/^#### (.+)$/gm, '<h4>$1</h4>');
  working = working.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  working = working.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  working = working.replace(/^# (.+)$/gm, '<h1>$1</h1>');
  working = working.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  working = working.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  working = working.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
  working = working.replace(/^- (.+)$/gm, '<li>$1</li>');
  working = working.replace(/(<li>.*<\/li>\n?)+/g, (block) => `<ul>${block}</ul>`);
  working = working.replace(/\n\n+/g, '</p><p>');
  working = `<p>${working.replace(/\n/g, '<br />')}</p>`;

  placeholders.forEach((block, index) => {
    working = working.replace(`@@CODEBLOCK_${index}@@`, block);
  });

  return working;
}

export function paginateItems<T>(items: T[], pageIndex: number, pageSize: number): T[] {
  const start = pageIndex * pageSize;
  return items.slice(start, start + pageSize);
}
