import os
import re

def remove_comments(text):
    # Group 1: String or Char literals
    # Group 2: Comments (Block or Line)
    pattern = r'("(?:\\.|[^\\"])*"|\'(?:\\.|[^\\\'])*\')|(/\*.*?\*/|//[^\n]*)'
    
    def replacer(match):
        if match.group(1) is not None:
            return match.group(1)
        return ''
    
    return re.sub(pattern, replacer, text, flags=re.DOTALL)

def cleanup_empty_lines(text):
    # Replace lines that consist of only whitespace with nothing, leaving only single newlines
    text = re.sub(r'^[ \t]+$', '', text, flags=re.MULTILINE)
    # Collapse more than 2 consecutive newlines into 2
    text = re.sub(r'\n{3,}', '\n\n', text)
    return text

def main():
    base_dir = r"e:\Diploma\AlgorithmRecipe\src"
    count = 0
    for root, dirs, files in os.walk(base_dir):
        for f in files:
            if f.endswith('.java'):
                path = os.path.join(root, f)
                with open(path, 'r', encoding='utf-8') as file:
                    content = file.read()
                
                new_content = remove_comments(content)
                new_content = cleanup_empty_lines(new_content)
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as file:
                        file.write(new_content)
                    print(f"Updated {path}")
                    count += 1
    print(f"Updated {count} files")

if __name__ == "__main__":
    main()
