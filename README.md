# Ticklist Climbing

Offline-first Android app for tracking bouldering and climbing routes, personal progress and climbing competitions.

## About

Ticklist Climbing is an open-source Android app for managing bouldering and lead-climbing routes completely offline.

The app is designed as a simple ticklist first, while providing a foundation for personal climbing logs and offline climbing competitions.

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

### Personal Progress

- Project
- Zone
- Top
- Flash
- Onsight
- Redpoint
- Repeat ascent / confirmed
- Optional completion date
- Optional repeat-ascent date
- Editable progress and dates
- Attempt tracking

The status **confirmed** means that the route was completed again after the first successful ascent.

### Competitions

- Create separate climbing competitions
- Share a competition structure without personal progress
- Import shared route collections
- Support multiple local user profiles
- Track the progress of multiple participants
- Display or edit imported participant progress
- Calculate rankings and totals
- Configure scoring rules for each competition

### Import and Export

- Offline ZIP import and export
- Export routes and photos without personal progress
- Export routes, photos and progress
- Import complete competition structures
- Import participant progress separately
- Versioned export format for future compatibility

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

Photos and complete route collections are exchanged using ZIP files rather than QR codes.

## Offline-First

Ticklist Climbing is designed to work without an internet connection.

- No account or server is required for the basic features
- Routes and photos are stored locally on the device
- Competition data can be exchanged manually
- ZIP files and QR codes are used for sharing data between devices

## Data Model

The application separates the following types of data:

- Route data
- Personal progress
- Competition data
- Competition participant progress
- Scoring rules

This allows the same route to be used in multiple competitions without mixing personal and competition progress.

## Technology

The planned technology stack includes:

- Kotlin
- Jetpack Compose
- Room / SQLite
- Android Photo Picker
- ZIP import and export
- QR-code generation and scanning
- GitHub Actions for automated builds

## Project Status

The project is currently in the planning and early development phase.

Planned development stages:

1. Basic route management
2. Offline progress tracking
3. Photos and completion dates
4. Search and filtering
5. ZIP import and export
6. Competition structures
7. User profiles
8. QR-code progress sharing
9. Configurable scoring and rankings
10. Stable first release

## Contributing

Contributions, ideas, bug reports and feature requests are welcome.

Please use GitHub Issues for discussions and bug reports. Pull requests should include a clear description of the proposed changes.

## License

This project is licensed under the GNU General Public License version 3 or later.

See the [LICENSE](LICENSE) file for the full license text.

Copyright (C) 2026 Lukas Holl
