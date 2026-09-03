-- The day a series starts. Two jobs:
--   1. nothing is generated before it, so "starts 3 September" works for any frequency;
--   2. it anchors INTERVAL (every other week). The materialiser expands through a sliding
--      window, so parity measured from the window would reset on every run — it has to be
--      measured from a fixed point.
-- NULL keeps existing rules behaving exactly as before.
ALTER TABLE recurrence_rule ADD COLUMN starts_on DATE;
