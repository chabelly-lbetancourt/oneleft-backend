"""The features of a candidate, in the order the model reads them. notifications computes the same ones (HU-012)."""
FEATURES = [
    'distanceKm',        # from the zone of the person to the meeting point
    'distanceRatio',     # distance / radius the person chose: 1.0 is the edge of their zone
    'minutesToStart',    # how soon the plan starts
    'localHour',         # hour of the day when the notice would arrive (0-23)
    'weekday',           # 0 = Monday ... 6 = Sunday
    'explicitInterest',  # 1 if the activity is in the person's list, 0 if they take every activity
    'viaAlert',          # 1 if they asked for it with a saved alert
    'pastRate',          # (accepted + 1) / (sent + 4) over their recent notices: 0.25 for someone new
    'pastSent',          # how many notices they have received recently
    'sentToday',         # notices received today so far
    'freeSpots',         # spots left in the plan
]
PRIOR_ACCEPTED = 1
PRIOR_SENT = 4
