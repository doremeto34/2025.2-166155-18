# -*- coding: utf-8 -*-
import os
import sys
sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')
from docx import Document

MATERIALS_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(MATERIALS_DIR, "extracted")
os.makedirs(OUTPUT_DIR, exist_ok=True)

DOCX_FILES = [
    "24- Nguyễn Trí Dũng.docx",
    "Document.docx",
    "SRS_LeHoangTung.docx",
    "SRS_NguyenThienNam.docx",
    "SRS_NguyenVuMinh.docx",
    "UC33.docx",
]


def extract_docx(filepath):
    """Extract all text from paragraphs and tables in a .docx file."""
    doc = Document(filepath)
    lines = []

    # We need to iterate through the document body in order,
    # so paragraphs and tables appear in their original sequence.
    for element in doc.element.body:
        tag = element.tag.split('}')[-1] if '}' in element.tag else element.tag

        if tag == 'p':
            # It's a paragraph
            from docx.oxml.ns import qn
            texts = []
            for run in element.iter(qn('w:t')):
                if run.text:
                    texts.append(run.text)
            line = ''.join(texts)
            lines.append(line)

        elif tag == 'tbl':
            # It's a table - extract cell text row by row
            from docx.table import Table
            from docx.oxml.ns import qn
            table = Table(element, doc)
            for row_idx, row in enumerate(table.rows):
                cell_texts = []
                for cell in row.cells:
                    cell_text = cell.text.strip()
                    cell_texts.append(cell_text)
                row_line = " | ".join(cell_texts)
                lines.append(row_line)
            lines.append("")  # blank line after table

    return '\n'.join(lines)


def main():
    for fname in DOCX_FILES:
        fpath = os.path.join(MATERIALS_DIR, fname)
        if not os.path.exists(fpath):
            print(f"[SKIP] File not found: {fname}")
            continue

        print(f"[EXTRACTING] {fname} ...")
        try:
            text = extract_docx(fpath)
            out_name = os.path.splitext(fname)[0] + ".txt"
            out_path = os.path.join(OUTPUT_DIR, out_name)
            with open(out_path, 'w', encoding='utf-8') as f:
                f.write(text)
            # Count lines
            line_count = text.count('\n') + 1
            print(f"  -> Saved to {out_name} ({line_count} lines, {len(text)} chars)")
        except Exception as e:
            print(f"  [ERROR] {fname}: {e}")
            import traceback
            traceback.print_exc()

    print("\n[DONE] All files processed.")


if __name__ == '__main__':
    main()
