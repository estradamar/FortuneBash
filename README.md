# FortuneBash

**FortuneBash** is a fun, interactive mock Linux terminal for Android. It brings the classic Unix `fortune` command to your pocket, bundled with a fully functional fake shell, custom commands, easter eggs, and a customizable retro UI.

Inspired by the Linux terminal culture and the EV3 *ev3dev* OS environment.

## Features
- **Authentic Terminal Feel:** Monospace fonts, dynamic user prompt (e.g., `guest@android:~$`), and auto-scrolling just like a real shell.
- **Classic Fortune Cookie:** Parses standard Unix `fortune` text files (delimited by `%`).
- **Category Management:** View, enable, or disable different quote categories on the fly.
- **Safety State Machine:** Built-in confirmation prompts `[y/N]` before enabling sensitive/NSFW categories from the `off/` directory.
- **Retro CMD Colors:** Customize background and text colors using the classic Windows CMD 2-character hex codes.
- **Easter Eggs & Utilities:** Dice rolling, Matrix effects, and more!

## Supported Commands

| Command | Description | Example |
| :--- | :--- | :--- |
| `help` | Shows the list of available commands and instructions. | `help` |
| `fortune` | Displays a random quote from your active categories. | `fortune` |
| `categories` | Lists all loaded text files and their status (e.g., `[X] pets`, `[ ] off/humor`). | `categories` |
| `toggle` | Enables or disables a specific category. | `toggle science` |
| `color` | Changes BG and text colors using CMD hex codes (0-F). | `color 0A` *(Black BG, Green Text)* |
| `color list`| Displays the classic Windows CMD color attribute list. | `color list` |
| `login` | Changes the current username and updates the terminal prompt. | `login hacker` |
| `whoami` | Prints the current username. | `whoami` |
| `date` | Prints the current system date and time. | `date` |
| `echo` | Repeats whatever text you type after the command. | `echo Hello World` |
| `roll` | Rolls dice using RPG notation (NdX). | `roll 2d6` |
| `yesorno` | Randomly answers "yes" or "no". | `yesorno` |
| `matrix` | Simulates a brief digital rain effect. | `matrix` |
| `clear` | Clears the terminal history. | `clear` |

*(Try using `sudo` with an invalid command and see what happens...)*

## How to Build & Setup

Because this app relies on the classic Unix `fortune` databases, you need to add the data files before building the project:

1. Clone this repository:
   `git clone https://github.com/your-username/FortuneBash.git`

2. Download your preferred fortune data files (text files delimited by `%`). You can find the classic ones at the [shlomif/fortune-mod repository](https://github.com/shlomif/fortune-mod/tree/master/fortune-mod/datfiles).

3. Place the clean categories (like `computers`, `science`, `art`) inside the Android project at:
   `app/src/main/assets/datfiles/`

4. Place any sensitive/humor categories inside:
   `app/src/main/assets/datfiles/off/`

5. Open the project in **Android Studio**, sync Gradle, build, and run!

## Credits
- Fortune databases and original logic inspired by the Unix `fortune` program (maintained by Shlomi Fish).
- Developed in Java for Android natively.

---
*This is not a real terminal, it's just for fun.*
