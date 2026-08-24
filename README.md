# Ticklist Climbing

Offline-first Android app for tracking bouldering and climbing routes, personal progress and climbing competitions.

## About

Ticklist Climbing is an open-source Android app for managing bouldering and lead-climbing routes completely offline.

The app starts as a simple and fast ticklist for climbing sessions and will gradually evolve into a personal climbing log and offline competition tool.

## Current Features

The current prototype includes:

- 90 numbered routes from `01` to `90`
- Bouldering progress statuses:
  - Flash
  - Top
  - Zone
  - Project
- Two-second long-press confirmation for status changes
- Animated progress line around the button border
- Route editing
- Route deletion
- Difficulty input
- Sorting by route number or status
- Filtering by status
- Offline operation without an account or server

## Planned Features

### Routes

- Bouldering and lead-climbing routes
- Route name or number
- Difficulty / grade
- Color and wall area
- Route description and personal notes
- One assigned route photo
- Multiple additional photos
- Editable route details
- Route discipline selection

### Personal Progress

For bouldering:

- Project
- Zone
- Top
- Flash

For lead climbing:

- Project
- Onsight
- Flash
- Redpoint
- Other configurable ascent types

Additional personal progress data:

- Repeat ascent / confirmed status
- Optional completion date
- Optional repeat-ascent date
- Editable progress and dates
- Attempt tracking
- Personal notes

The status **confirmed** means that the route was completed again after the first successful ascent.

### Competitions

- Create separate climbing competitions
- Define a competition-specific route collection
- Share the competition structure without personal progress
- Import shared route collections
- Support multiple local user profiles
- Track the progress of multiple participants
- Display imported participant progress
- Edit participant progress where permitted
- Calculate rankings and totals
- Configure scoring rules for each competition
- Recalculate scores when scoring rules change

### Import and Export

- Offline ZIP import and export
- Export routes and photos without personal progress
- Export routes, photos and personal progress
- Import complete competition structures
- Import participant progress separately
- Support competition data with or without progress
- Versioned export format for future compatibility
- Conflict handling when importing existing progress

### QR-Code Progress Sharing

Competition progress can be shared between devices using a QR code.

A progress QR code may contain:

- Competition ID
- User ID
- Username
- Route results
- Optional attempt counts
- Timestamp
- Data format version
- Checksum

Photos and complete route collections will be exchanged using ZIP files rather than QR codes.

The QR code is intended for compact progress data, while ZIP files are intended for complete backups and route collections.

## Offline-First

Ticklist Climbing is designed to work without an internet connection.

- No account or server is required for the basic features
- Routes and progress are stored locally on the device
- Photos will be stored locally on the device
- Competition data can be exchanged manually
- ZIP files and QR codes are used for sharing data between devices
- Internet access is not required during normal use

## Data Model

The application separates the following types of data:

- Route data
- Route photos
- Personal progress
- Competition data
- Competition route collections
- Competition participants
- Competition participant progress
- Scoring rules

This separation allows the same route to be used in multiple competitions without mixing personal and competition progress.

## Planned Technology

- Kotlin
- Jetpack Compose
- Room / SQLite for local storage
- Android Photo Picker
- Local file storage for photos
- ZIP import and export
- QR-code generation and scanning
- GitHub Actions for automated builds

## Project Status

The project is currently in early development.

### Version 0.01

The first prototype currently provides:

1. A list of 90 routes
2. Route numbers from `01` to `90`
3. Difficulty input
4. Route editing and deletion
5. Flash, Top, Zone and Project statuses
6. Two-second long-press status confirmation
7. Animated progress around the status button border
8. Sorting by route number or status
9. Status filtering
10. Offline operation

### Known Limitations

- Route data is currently not permanently stored after restarting the app
- Photos are not implemented yet
- Completion dates are not implemented yet
- Competition management is not implemented yet
- User profiles are not implemented yet
- QR-code sharing is not implemented yet
- ZIP import and export are not implemented yet
- Scoring formulas and rankings are not implemented yet
- The current version is a development prototype and not a stable release

### Planned Development Stages

1. Basic route management
2. Permanent offline storage
3. Personal progress tracking
4. Photos and completion dates
5. Search and filtering improvements
6. ZIP import and export
7. Competition structures
8. User profiles
9. Participant progress sharing
10. QR-code progress sharing
11. Configurable scoring and rankings
12. Stable first release

## Contributing

Contributions, ideas, bug reports and feature requests are welcome.

Please use GitHub Issues for discussions, bug reports and feature requests.

Pull requests should include:

- A clear description of the proposed changes
- The reason for the change
- Information about testing
- Screenshots where appropriate

## License

This project is licensed under the GNU General Public License version 3 or later.

See the [LICENSE](LICENSE) file for the full license text.

Copyright (C) 2026 Lukas Holl  
GitHub: [@charliesz](https://github.com/charliesz)


