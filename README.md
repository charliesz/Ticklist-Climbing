# Ticklist Climbing

Offline-first Android app for tracking bouldering routes, personal progress, route photos, collections and climbing competitions.

## About

Ticklist Climbing is an open-source Android app for managing bouldering and lead-climbing routes completely offline.

The app started as a fast personal ticklist and is being developed into a flexible climbing log and offline competition tool.

## Current Features

### Collections

- Multiple bouldering collections
- Create new collections
- Create collections with a configurable number of routes
- Open collections from the overview
- Rename collections by long-pressing a collection card
- Delete collections including their routes and cover photos
- Collection notes for competition data and other information
- Collection cover photo and thumbnail
- Initial collection with 90 routes numbered from `01` to `90`
- Portrait orientation lock

### Routes

- Route number and name
- Difficulty / grade input
- Route notes
- Route editing
- Route deletion
- Route-specific persistent offline storage
- Separate route lists for different collections
- Route notes with Markdown-style links
- Clickable links in displayed notes

Example:

```markdown
[Competition results](https://example.com/results)
```

The app displays `Competition results` as a clickable link.

### Personal Progress

- Project
- Zone
- Top
- Flash
- Protected status changes using a configurable long press
- Configurable status-confirmation duration from 0 to 3 seconds
- Status-confirmation intervals of 0.25 seconds
- Haptic feedback after successful status changes
- Haptic feedback can be enabled or disabled in the settings
- Stored status-entry date
- Editable completion date for Top and Flash
- Completion dates remain available after restarting the app
- Progress transfer between collections
- Transfer options:
  - overwrite target progress
  - fill only empty target fields

### Route Photos

- Photo selection from the device
- Local photo storage
- One main photo per route
- Additional route photos
- Route photo gallery in the edit dialog
- Set a photo as the main photo
- Delete route photos
- Original images for the gallery and viewer
- Optimized thumbnails for route previews
- Main-photo thumbnail in the route overview
- Zoomable route photo viewer
- Pan and pinch-to-zoom support
- Short tap on a route thumbnail opens the viewer
- Long press on a route photo opens route editing
- Empty photo placeholders remain available for route editing

### Collection Photos

- Collection cover photo
- Separate cover thumbnail
- Original cover image for the viewer
- Cover thumbnail in the collection overview
- Cover thumbnail in the open collection view
- Replace or delete collection cover photos
- Cover photo and thumbnail are stored locally

### Filtering and Sorting

- Multi-select filters:
  - Flash
  - Top
  - Zone
  - Project
  - No status
- Quick filter actions:
  - All
  - None
  - Hide completed routes
- Completed routes are defined as Top and Flash
- Zone and Project routes remain visible when completed routes are hidden
- Clickable route headers for ascending and descending sorting
- Sorting by:
  - Route number
  - Flash
  - Top
  - Zone
  - Project

### Bulk Editing

- Checkbox-based multi-route selection
- Select routes through the route card in selection mode
- Apply status changes to multiple routes
- Apply completion-date changes to multiple routes
- Clear completion dates for multiple routes
- Transfer progress between collections
- Copy selected routes into a separate collection, including route information and progress
- Delete selected routes from the edit dialog

### Calculation Bar

The bottom calculation bar displays:

```text
Top: total Tops including Flash
Flash: Flash count in brackets
Zone: Zones without Top
```

Example:

```text
Top: 70 (56 Flash) · Zone: 3
```

Filtering only changes the visible routes. The calculation includes all routes in the active collection.

### Settings

The app provides a settings area accessible through the menu:

- Haptic feedback on/off
- Configurable status-confirmation duration
- Light or dark appearance
- Motivational feedback on/off via the `Debug-Info` setting
- Full backup creation
- Full backup restoration
- About Ticklist Climbing page
- Installed app version
- Link to the GitHub repository
- Link to GitHub releases

### Motivational Feedback

The app can display short motivational messages after successful manual route entries.

- Triggered by manual Flash or Top entries
- Bulk edits do not trigger the messages
- Messages can be disabled in the settings
- Messages are selected in a shuffled order
- A message is not repeated until the available message list has been used
- The remaining message order is stored locally

## Import and Export

### Collection Export

Collections can be exported as ZIP files.

A collection export contains:

- Collection name
- Discipline
- Collection notes
- Collection cover photo and thumbnail
- Route numbers
- Route names
- Difficulties
- Route notes
- Main route photos
- Additional route photos
- Route thumbnails

Personal progress is not included in a collection export.

Example structure:

```text
collection.zip
├── manifest.json
├── collection.json
├── routes.json
└── photos/
    ├── collection/
    │   ├── cover.jpg
    │   └── cover-thumbnail.webp
    └── route_<route-id>/
        ├── photo_<photo-id>.jpg
        └── photo_<photo-id>-thumbnail.webp
```

The ZIP export is created locally first and copied to the selected destination only after the archive has been fully closed. This prevents incomplete archives during large exports.

Collection export operations run in the background so that the user interface remains responsive during large exports.

### Collection Import

Collections can be imported from ZIP files.

Imported collections are created as new collections:

```text
Münster United 2026
Münster United 2026_import
Münster United 2026_import_2
```

The import does not overwrite existing collections.

The import includes:

- Collection data
- Collection notes
- Collection cover photo
- Routes
- Route notes
- Route photos
- Route thumbnails

Personal progress is not imported automatically.

Collection import operations run in the background so that the user interface remains responsive during large imports.

### Full Backup

The app supports complete backup and restore functionality.

A full backup contains:

- All collections
- Collection notes
- Collection cover photos and thumbnails
- All routes
- Route notes
- Route status and progress
- Status-entry dates
- Completion dates
- Route photos
- Route thumbnails
- Application settings

Example structure:

```text
ticklist-full-backup.zip
├── manifest.json
├── collections.json
├── routes.json
├── route_photos.json
├── app_settings.json
└── photos/
    ├── collections/
    │   └── <collection-id>/
    │       ├── cover.jpg
    │       └── cover-thumbnail.webp
    └── routes/
        └── <route-id>/
            ├── photo_<photo-id>.jpg
            └── photo_<photo-id>-thumbnail.webp
```

Full backup export and restore operations run in the background.

A full backup should be created before:

- uninstalling the app
- changing devices
- resetting the device
- performing major app updates

### Full Backup Restore

A full backup can be selected through the Android file picker and restored locally.

The restore process validates the backup manifest and restores:

- collections
- routes
- progress
- photos
- thumbnails
- application settings

The current local data is replaced during a full restore. Create a current full backup before restoring another backup if the existing data needs to be preserved.

### Progress Transfer

Progress can be transferred from one collection to another.

The source collection is selected from the currently opened collection. The target collection is selected separately.

Before transferring, the app checks:

- Source and target are different collections
- Route counts are identical
- Route numbers can be matched

The following data can be transferred:

- Status
- Status-entry date
- Completion date

Photos, notes, route names and difficulties in the target collection remain unchanged.

### Test Database

A test database export is attached to the GitHub release `v0.10` for testing the import functionality.

The test export contains:

- Sample collections
- Routes
- Collection notes
- Route notes
- Photos
- Thumbnails

## Offline-First

Ticklist Climbing is designed to work without an internet connection.

- No online account is required for the basic features
- No server is required for normal use
- Routes and progress are stored locally
- Photos are stored in the app-private device storage
- Collections can be managed offline
- ZIP files can be imported and exported manually
- Internet access is only needed for external links, GitHub access or future update checks

## Data Model

```text
Collection
├── ID
├── Name
├── Discipline
├── Notes
├── Cover photo
├── Cover thumbnail
└── Created date

Route
├── Stable internal route ID
├── Collection ID
├── Route number
├── Name
├── Difficulty
├── Notes
├── Status
├── Status changed at
└── Completion date

Route Photo
├── Stable photo ID
├── Route ID
├── Original file path
├── Thumbnail file path
├── Main-photo flag
├── Optional crop data
└── Created date
```

This structure allows multiple collections to contain the same visible route number, such as route `01`.

## Planned Features

### Lead Climbing

- Lead-climbing collections
- Onsight
- Flash
- Redpoint
- Other configurable ascent types
- Repeat ascent / confirmed status
- Attempt tracking
- Lead-climbing-specific calculations

### Competition Features

- Competition-specific collections
- Local user profiles
- Stable user IDs
- Participant progress
- Display and edit participant progress
- Roles and permissions
- QR-code-based progress sharing
- Competition rankings
- Configurable scoring methods
- Competition-specific formulas

### Collection and Route Improvements

- Better full-screen photo navigation
- Additional photo metadata
- Improved thumbnail generation for existing photos
- Optional photo crop selection
- Route color and wall area
- More detailed route descriptions
- Video links and richer Markdown support

### Updates

- Check for new GitHub releases
- Display available update information
- Open release notes
- Download and install signed updates

## Project Status

Ticklist Climbing `v1.00` is the first stable release.

The current release includes:

1. Offline Room database
2. Multiple collections
3. Collection notes and cover photos
4. Persistent route progress
5. Editable completion dates
6. Status-entry dates
7. Filters and sorting
8. Checkbox-based bulk editing
9. Progress transfer between collections
10. Route photos and thumbnails
11. Zoomable photo viewer
12. Collection ZIP export
13. Collection ZIP import
14. Full backup export
15. Full backup restore
16. Settings and DataStore
17. Theme selection
18. Haptic feedback settings
19. Motivational feedback
20. Route and collection notes
21. About page with installed version and GitHub links
22. Background processing for import, export and backup operations

### Known Limitations

- QR-code progress sharing is not implemented yet
- Competition participant management is not implemented yet
- Lead-climbing-specific workflows are not complete
- Photo crop editing is not implemented yet
- Update checking is not implemented yet
- Released APKs are currently debug builds for testing

## Contributing

Contributions, ideas, bug reports and feature requests are welcome.

Please use GitHub Issues for:

- Bug reports
- Feature requests
- UI suggestions
- Import/export discussions
- Competition scoring ideas
- New climbing disciplines

Pull requests should include:

- A clear description of the proposed change
- The reason for the change
- Information about testing
- Screenshots for visual changes where appropriate

## License

This project is licensed under the GNU General Public License version 3 or later.

See the [LICENSE](LICENSE) file for the full license text.

Copyright (C) 2026 charliesz
