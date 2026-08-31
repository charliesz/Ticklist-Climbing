# Ticklist Climbing

Offline-first Android app for tracking bouldering routes, personal progress, route photos and climbing competitions.

## About

Ticklist Climbing is an open-source Android app for managing bouldering and lead-climbing routes completely offline.

The app starts as a fast personal ticklist and is being developed into a flexible climbing log and offline competition tool.

## Current Features

### Collections

- Multiple bouldering collections
- Create new collections
- Open collections from the overview
- Rename collections by long-pressing a collection card
- Delete collections including their routes
- Initial collection with 90 routes numbered from `01` to `90`

### Routes

- Route number and name
- Difficulty input
- Route editing
- Route deletion
- Route-specific persistent offline storage
- Separate route lists for different collections

### Personal Progress

- Project
- Zone
- Top
- Flash
- Protected status changes using a 1.5-second long press
- Haptic feedback after successful status changes
- Stored status-entry date
- Editable completion date for Top and Flash
- Completion dates remain available after restarting the app

### Route Photos

- Photo selection from the device
- Local photo storage
- One main photo per route
- Additional route photos
- Route photo gallery in the edit dialog
- Set a photo as the main photo
- Delete route photos
- Main-photo thumbnail in the route overview
- Zoomable route photo viewer
- Long press on a route photo opens route editing

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
- Clickable route headers for ascending and descending sorting
- Sorting by:
  - Route number
  - Flash
  - Top
  - Zone
  - Project

### Bulk Editing

- Checkbox-based multi-route selection
- Apply status changes to multiple routes
- Apply completion-date changes to multiple routes
- Clear completion dates for multiple routes

### Calculation Bar

The bottom calculation bar displays:

