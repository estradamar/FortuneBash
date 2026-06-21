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

## Privacy Policy

**Effective Date:** June 21, 2026

**1. Data Collection and Usage**
FortuneBash does not collect, store, transmit, or share any personal user data. All features, commands, and games within the app operate entirely offline and locally on your device.

**2. Network Permissions (`ACCESS_NETWORK_STATE`)**
The application requests the `ACCESS_NETWORK_STATE` permission solely to power the `ipconfig` in-game command. This command queries your device's local network interface to display local IP information as part of the terminal simulation experience. This information is printed locally to your screen and is never logged, tracked, or sent to any external servers or third-party services.

**3. Third-Party Services**
FortuneBash does not use any third-party analytics or advertising SDKs. 

**4. Children's Privacy**
Our service does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from children under 13.

**5. Changes to This Privacy Policy**
We may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes.

---
*For any questions regarding this privacy policy, please open an issue in this repository.*


## Credits
- Fortune databases and original logic inspired by the Unix `fortune` program (maintained by Shlomi Fish).
- Developed in Java for Android natively.

---
*This is not a real terminal, it's just for fun.*
