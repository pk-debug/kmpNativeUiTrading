"""Enforce documented sources and domain/presentation dependency direction."""
from pathlib import Path
import re


def violations(path, text):
    """Inspect imports and fully qualified references, including Kotlin aliases."""
    failures = []
    if not text.lstrip().startswith('/**'):
        failures.append('missing file responsibility documentation')
    dependencies = re.findall(r'^import (.+)$', text, re.MULTILINE)
    dependencies += re.findall(r'com\.pawan\.kmpnativeuitrading\.[\w.]+', text)
    if '/domain/' in path:
        forbidden = ['.data.', '.presentation.', 'io.ktor', 'android.', 'platform.']
    elif '/presentation/' in path or '/ui/' in path:
        forbidden = ['.data.', '.core.platform.', 'io.ktor', 'java.net.', 'platform.']
    else:
        forbidden = []
    for dependency in set(dependencies):
        if any(part in dependency for part in forbidden):
            failures.append(f'forbidden implementation dependency {dependency}')
    return failures


def main():
    """Report exact paths and return nonzero for architecture violations."""
    root = Path(__file__).resolve().parents[1]
    sources = []
    for folder in ['sharedLogic/src', 'androidApp/src', 'sharedUI/src', 'iosApp']:
        sources.extend(p for p in (root / folder).rglob('*') if p.suffix in {'.kt', '.swift'} and 'build' not in p.parts)
    failures = [f'{p.relative_to(root)}: {error}' for p in sources
                for error in violations('/' + p.relative_to(root).as_posix(), p.read_text())]
    if failures:
        raise SystemExit('\n'.join(failures))
    print(f'Architecture checks passed for {len(sources)} Kotlin/Swift source files')


if __name__ == '__main__':
    main()
