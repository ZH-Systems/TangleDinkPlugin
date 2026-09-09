from pathlib import Path
import argparse
import tiktoken


def strip_java_comments(source: str) -> str:
    """
    Remove Java //, /* */, and /** */ comments while preserving:
    - normal string literals
    - character literals
    - Java text blocks
    - line breaks

    Preserving line breaks prevents unrelated lines from being joined together.
    """

    result = []

    NORMAL = 0
    LINE_COMMENT = 1
    BLOCK_COMMENT = 2
    STRING = 3
    CHAR = 4
    TEXT_BLOCK = 5

    state = NORMAL
    i = 0
    length = len(source)

    while i < length:
        char = source[i]
        next_char = source[i + 1] if i + 1 < length else ""

        if state == NORMAL:
            # Java text block: """
            if source.startswith('"""', i):
                result.append('"""')
                i += 3
                state = TEXT_BLOCK
                continue

            # Standard string
            if char == '"':
                result.append(char)
                state = STRING
                i += 1
                continue

            # Character literal
            if char == "'":
                result.append(char)
                state = CHAR
                i += 1
                continue

            # Single-line comment
            if char == "/" and next_char == "/":
                state = LINE_COMMENT
                i += 2
                continue

            # Block/Javadoc comment
            if char == "/" and next_char == "*":
                state = BLOCK_COMMENT
                i += 2
                continue

            result.append(char)
            i += 1
            continue

        if state == LINE_COMMENT:
            if char == "\n":
                result.append("\n")
                state = NORMAL

            i += 1
            continue

        if state == BLOCK_COMMENT:
            if char == "*" and next_char == "/":
                state = NORMAL
                i += 2
                continue

            # Preserve newlines so source structure remains roughly intact.
            if char == "\n":
                result.append("\n")

            i += 1
            continue

        if state == STRING:
            result.append(char)

            # Escape sequence
            if char == "\\" and i + 1 < length:
                result.append(source[i + 1])
                i += 2
                continue

            if char == '"':
                state = NORMAL

            i += 1
            continue

        if state == CHAR:
            result.append(char)

            # Escape sequence
            if char == "\\" and i + 1 < length:
                result.append(source[i + 1])
                i += 2
                continue

            if char == "'":
                state = NORMAL

            i += 1
            continue

        if state == TEXT_BLOCK:
            if source.startswith('"""', i):
                result.append('"""')
                i += 3
                state = NORMAL
                continue

            result.append(char)
            i += 1
            continue

    return "".join(result)


def get_encoding(encoding_name: str):
    try:
        return tiktoken.get_encoding(encoding_name)
    except Exception as ex:
        raise RuntimeError(
            f"Could not load tokenizer encoding '{encoding_name}'."
        ) from ex


def count_tokens(text: str, encoding) -> int:
    return len(encoding.encode(text))


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Count estimated LLM tokens in Java files under a source directory, "
            "with Java comments removed."
        )
    )

    parser.add_argument(
        "source",
        nargs="?",
        default="src",
        help="Source directory to scan. Default: ./src",
    )

    parser.add_argument(
        "--encoding",
        default="cl100k_base",
        help=(
            "tiktoken encoding to use. "
            "Default: cl100k_base. "
            "Use o200k_base for newer OpenAI models."
        ),
    )

    parser.add_argument(
        "--details",
        action="store_true",
        help="Print a per-file token breakdown.",
    )

    args = parser.parse_args()

    source_directory = Path(args.source).resolve()

    if not source_directory.exists():
        print(f"ERROR: Directory does not exist: {source_directory}")
        return

    if not source_directory.is_dir():
        print(f"ERROR: Path is not a directory: {source_directory}")
        return

    encoding = get_encoding(args.encoding)

    java_files = sorted(source_directory.rglob("*.java"))

    if not java_files:
        print(f"No .java files found under: {source_directory}")
        return

    total_files = 0
    total_raw_characters = 0
    total_code_characters = 0
    total_raw_tokens = 0
    total_code_tokens = 0

    file_results = []

    for java_file in java_files:
        try:
            source = java_file.read_text(
                encoding="utf-8",
                errors="replace",
            )
        except Exception as ex:
            print(f"WARNING: Could not read {java_file}: {ex}")
            continue

        stripped_source = strip_java_comments(source)

        raw_characters = len(source)
        code_characters = len(stripped_source)

        raw_tokens = count_tokens(source, encoding)
        code_tokens = count_tokens(stripped_source, encoding)

        total_files += 1
        total_raw_characters += raw_characters
        total_code_characters += code_characters
        total_raw_tokens += raw_tokens
        total_code_tokens += code_tokens

        file_results.append(
            (
                java_file,
                raw_tokens,
                code_tokens,
                raw_characters,
                code_characters,
            )
        )

    comments_removed_characters = (
        total_raw_characters - total_code_characters
    )

    comments_removed_tokens = (
        total_raw_tokens - total_code_tokens
    )

    if args.details:
        print()
        print("Per-file token counts")
        print("=" * 100)

        for (
            java_file,
            raw_tokens,
            code_tokens,
            raw_characters,
            code_characters,
        ) in file_results:
            relative_path = java_file.relative_to(source_directory)

            print(
                f"{str(relative_path):70} "
                f"raw={raw_tokens:10,} "
                f"stripped={code_tokens:10,}"
            )

    print()
    print("=" * 60)
    print("JAVA TOKEN COUNT")
    print("=" * 60)
    print(f"Source directory:            {source_directory}")
    print(f"Tokenizer:                   {args.encoding}")
    print()
    print(f"Java files:                  {total_files:,}")
    print()
    print(f"Raw characters:              {total_raw_characters:,}")
    print(f"Characters after stripping:  {total_code_characters:,}")
    print(f"Comment characters removed:  {comments_removed_characters:,}")
    print()
    print(f"Raw estimated tokens:        {total_raw_tokens:,}")
    print(f"Tokens after stripping:      {total_code_tokens:,}")
    print(f"Comment tokens removed:      {comments_removed_tokens:,}")
    print()
    print(
        f"FINAL ESTIMATED TOKEN COUNT: {total_code_tokens:,}"
    )
    print("=" * 60)


if __name__ == "__main__":
    main()