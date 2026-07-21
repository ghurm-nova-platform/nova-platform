import { escapeHtml, paginateItems, renderMarkdownPreview } from './knowledge-viewer.helper';

describe('KnowledgeViewerTest', () => {
  it('escapes HTML before rendering markdown', () => {
    const html = renderMarkdownPreview('<script>alert(1)</script>');
    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });

  it('renders headings, emphasis, and code blocks', () => {
    const markdown = '# Title\n\n**bold** text\n\n```\ncode line\n```';
    const html = renderMarkdownPreview(markdown);
    expect(html).toContain('<h1>Title</h1>');
    expect(html).toContain('<strong>bold</strong>');
    expect(html).toContain('<pre class="knowledge-engine__code-block"><code>code line</code></pre>');
  });

  it('escapes HTML entities', () => {
    expect(escapeHtml('<div>&"test"</div>')).toBe('&lt;div&gt;&amp;&quot;test&quot;&lt;/div&gt;');
  });

  it('paginates items for client-side paging', () => {
    const items = [1, 2, 3, 4, 5];
    expect(paginateItems(items, 0, 2)).toEqual([1, 2]);
    expect(paginateItems(items, 1, 2)).toEqual([3, 4]);
    expect(paginateItems(items, 2, 2)).toEqual([5]);
  });
});
