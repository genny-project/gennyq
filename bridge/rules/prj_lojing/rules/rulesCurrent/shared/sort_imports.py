import sys

# Color stuff
class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

# Usage
def print_usage():
    print("Usage: ")
    print("python sort_imports.py <FILENAME>")

# Return unique lines from a file and ignore comments/blank lines
def remove_duplicates(file):
    lines = []
    declaration = []
    endDeclaration = False
    duplicateCount = 0

    try:
        with open(file, 'r') as importsfile:
            for line in importsfile:
                strip_line = line.strip()

                # Don't start adding import lines until endDeclaration is reached
                if(not endDeclaration):
                    declaration.append(line)
                    if(line.strip()[0:3] == "end"):
                        endDeclaration = True
                    continue
                if(not strip_line):
                    continue
                if(strip_line[0:2] == "/*"):
                    continue

                if(strip_line not in lines):
                    lines.append(strip_line)
                else:
                    duplicateCount += 1
    except FileNotFoundError:
        print(f"{bcolors.FAIL}Unknown file: {file}{bcolors.ENDC}")
    print(f"{bcolors.OKGREEN}Found {bcolors.UNDERLINE}{bcolors.WARNING}{duplicateCount}{bcolors.ENDC}{bcolors.OKGREEN} duplicates!{bcolors.ENDC}")
    return (lines, declaration)

# Rewrite a file with a list of unique import statements, sorting by packages and adding comments
def generate_file(file, file_data):
    lines = file_data[0]
    declaration = file_data[1]
    packages = {}
    for line in lines:
        import_ref = line.split(" ")[1].split(".")
        package = ".".join(import_ref[0:-1])
        class_name = import_ref[-1][0:-1]
        if(package not in packages):
            packages[package] = []
        packages[package].append(class_name)

    with open(file, 'w') as exportsfile:
        # Write the rule declaration at the top
        for line in declaration:
            exportsfile.write(line)

        exportsfile.write("\n")
        # Write the imports below
        for package in packages:
            #print(f"{bcolors.OKGREEN}/* {package} */{bcolors.ENDC}")
            exportsfile.write("/* " + package + " */\n")
            for class_name in packages[package]:
                #print(f"{bcolors.HEADER}import{bcolors.ENDC} " + package + "." + class_name + ";")
                exportsfile.write("import " + package + "." + class_name + ";\n")
            exportsfile.write("\n")

if __name__ == "__main__":
    # Ensure only one arg passed into commandline
    if(len(sys.argv) != 2):
        print_usage()
    else:
        file_name = sys.argv[1]
        print(f"{bcolors.HEADER}Removing duplicates of: {file_name}{bcolors.ENDC}")
        lines = remove_duplicates(file_name)
        lines = sorted(lines)
        generate_file(file_name, lines)