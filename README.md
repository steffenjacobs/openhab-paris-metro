# Paris Metro Station Timetable Binding

This Binding can provide the timetable for a given metro line in paris for a given direction at a given station. The timetable shows the next departing trains for the selected train, direction and station.

## Supported Things

A Thing should be created for each train, direction and station to monitor the departure timetable of.

## Thing Configuration

Please enter the name of the train, direction and station you want to monitor in the configuration section of the Thing in the Paper UI.

## Channels

There is one RefreshParisTransportation channel, which refreshes the timetable. The updated timetable in then written onto the OutputParisTransportation channel.

## Disclaimer

Keep in mind that this is just a proof of concept and not production ready!
