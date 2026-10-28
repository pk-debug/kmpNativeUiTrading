"""Check source-file documentation and keep feature domain code platform independent."""
from pathlib import Path
import re


def main():
    """Fail CI with exact paths when a source header or domain boundary is missing."""
    root = Path(__file__).resolve().parents[1]
    failures = []
    sources = []
    for folder in ['sharedLogic/src', 'androidApp/src', 'sharedUI/src', 'iosApp']:
        sources.extend(p for p in (root / folder).rglob('*') if p.suffix in {'.kt', '.swift'} and 'build' not in p.parts)
    for path in sources:
        text = path.read_text()
        if not text.lstrip().startswith('/**'):
            failures.append(f'{path.relative_to(root)}: missing file responsibility documentation')
        if '/domain/' in path.as_posix():
            for imported in re.findall(r'^import (.+)$', text, re.MULTILINE):
                if any(part in imported for part in ['.data.', '.presentation.', 'io.ktor', 'android.', 'platform.']):
                    failures.append(f'{path.relative_to(root)}: domain imports implementation {imported}')
    if failures:
        raise SystemExit('\n'.join(failures))
    print(f'Architecture checks passed for {len(sources)} Kotlin/Swift source files')


if __name__ == '__main__':
    main()
