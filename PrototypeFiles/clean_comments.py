import os
import io
import tokenize
import re

def remove_comments(source):
    io_obj = io.StringIO(source)
    out = ""
    last_lineno = -1
    last_col = 0
    
    try:
        for tok in tokenize.generate_tokens(io_obj.readline):
            token_type = tok[0]
            token_string = tok[1]
            start_line, start_col = tok[2]
            end_line, end_col = tok[3]
            
            if start_line > last_lineno:
                last_col = 0
            if start_col > last_col:
                out += (" " * (start_col - last_col))
            
            if token_type == tokenize.COMMENT:
                pass
            else:
                out += token_string
                
            last_lineno = end_line
            last_col = end_col
    except tokenize.TokenError:
        return source
    
    # Clean up multiple empty lines
    lines = out.splitlines()
    clean_lines = []
    empty_count = 0
    for line in lines:
        if line.strip() == "":
            empty_count += 1
            if empty_count <= 2:
                clean_lines.append(line)
        else:
            empty_count = 0
            clean_lines.append(line)
    return "\n".join(clean_lines)

def main():
    directory = r"e:\Diploma\FoodParser"
    for filename in os.listdir(directory):
        if filename.endswith(".py") and filename != "clean_comments.py":
            filepath = os.path.join(directory, filename)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            
            # Clean Secrets
            content = re.sub(
                r'API_KEY\s*=\s*".*?"',
                'API_KEY = "YOUR_API_KEY_HERE"',
                content
            )
            content = re.sub(
                r'AZURE_CONNECTION_STRING\s*=\s*".*?"',
                'AZURE_CONNECTION_STRING = "YOUR_CONNECTION_STRING_HERE"',
                content
            )
            
            clean_content = remove_comments(content)
            
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(clean_content)
    print("Done cleaning comments and secrets.")

if __name__ == "__main__":
    main()
