# 🔗 Mini Linker

**Mini Linker** is a custom, two-pass linker written in Java (version 24+) that processes fictional `MINIOBJ` format object files. It resolves memory addresses, handles symbol definitions and external references, applies relocations (`ABS16` & `REL16`), and outputs a final memory image (`memory.hex`) alongside a detailed link map (`link.map`).

It features both a robust Command Line Interface (CLI) and a sleek graphical user interface (GUI) built with JavaFX and styled via Maven.

---

## ✨ Features
- **Custom Object Format (`MINIOBJ 1`)**: Parses specific object sections (`.text`, `.data`), word-level memory, symbols (`DEFINE`, `EXTERN`), and relocations (`RELOC`).
- **Two-Pass Linking Architecture**:
  - *Pass 1*: Section layout creation and global symbol table construction.
  - *Pass 2*: Relocation patching and generation of the final memory image.
- **JavaFX GUI**: Visually add/remove `.obj` modules, inspect module details (sections, definitions, unresolved symbols), and trigger the linking process with a single click.
- **Detailed Output**: Generates `memory.hex` for the linked machine code and `link.map` for memory layout tracking.

## 🛠️ Prerequisites
- **Java 24** or newer (with `--enable-preview` if required by your JDK).
- **Maven** (to handle dependencies and build the JavaFX application).

## 🚀 How to Run

### Using Maven (Recommended)
To launch the beautiful JavaFX Graphical Interface:
```bash
mvn clean javafx:run
```

### Running the CLI (Command Line)
If you prefer the terminal, you can compile and run the CLI directly. The CLI accepts a list of `.obj` files:
```bash
mvn clean compile
java -cp target/classes minilinker.Main file1.obj file2.obj
```

## 📁 Example Usage
The project includes an `examples/` folder with sample modules:
- `main_topla.obj`
- `math_topla.obj`

Load these two files into the GUI and click **"🔗 Link Modules!"** to see the linker resolve the `topla` symbol across both files and generate the final output.
