# SuperCollider Snippet Manager

A desktop app for organizing and playing your SuperCollider code snippets. Save your favorite synths, patterns, and effects in one place and trigger them with a click.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-23-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-green)

---

## What It Does

- **Play snippets directly** - Click play and hear your code without copy/pasting
- **Organize with folders and tags** - Keep your sounds organized your way
- **Search everything** - Find snippets by name, tag, or code content
- **38 example snippets included** - Synths, drums, patterns, effects ready to use
- **Clean, modern interface** - Easy to navigate and use

---

## Quick Start

### You'll Need

- Java 23 or later
- Maven 3.9+
- SuperCollider

### Install & Run
```bash
git clone https://github.com/CalumFleming/SCSnippetManager.git
cd SCSnippetManager
mvn clean package
mvn javafx:run
```

### First Time Setup

1. Boot SuperCollider server
2. Click "Setup SC" button in the app
3. Paste and run the code in SuperCollider
4. Done - start playing snippets

---

## Basic Usage

**Play a snippet:**
- Select it and click Play
- Press Cmd+. to stop all sounds

**Create a snippet:**
- Click New Snippet
- Add your code, give it a name and some tags
- Choose a folder to organize it

**Find snippets:**
- Use the search bar to find anything
- Click folders or tags to filter
- Click Clear Filters to see everything

**Share snippets:**
- Export button saves a snippet as JSON
- Import button loads someone else's snippet

---

## What's Included

38 ready-to-use examples:
- Synths (pads, bass, leads, drums)
- Patterns and sequencing
- Effects (reverb, delay, distortion)
- Ambient textures
- Experimental sounds

All organized in folders and tagged for easy browsing.

---

## Coming Next

- Keyboard shortcuts for faster workflow
- Favorites system
- More example snippets
- Code syntax highlighting
- SuperCollider server control from the app

---

## Contributing

Found a bug? Have an idea? Open an issue or submit a pull request.

---

## License

MIT License - use it however you want.
