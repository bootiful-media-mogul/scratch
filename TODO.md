# To Do  

## migrate to a new hosted SQL database. ElephantSQL is sunsetting. 
i also need to write a migration tool to migrate old podcasts to the new system and schema. this will be a good chance to move over _only_ that which is valid. i probably have lots of orphaned records in the old one. if i select only the podcasts that have valid data and only migrate thse, then we'll shed all the other junk. this would be a good chance to identify which S3 assets to keep, too.

## notes
it would be nice to have production notes available when creating an episode so if u need to revisit the episode you can see what you had done before. these notes should stick to whatever thing was active when you created the note. so maybe you took a note while editing a podcast episode. or you took a note while editing the whole podcast itself. or a blog. etc. 

## blogs 

we need to have a plugin that publishes things as blogs. but what things. well, how about we have a button that takes u to a blog composition screen (with side-by-side markdown and preview functionality?). you can edit the text and then choose from a list of plugins to publish the blog to contentful or github or whatever. does wordpress have an API lol? there needs to be some way of connecting a given blog with the podcast that inspired it. another nice thing we can do is have the blog preloaded with the text of the podcast episode, you know, as a starting point. you can use Ai to expand on it. 

## search
the old studio system had a search functionality that we could use to find all podcasts. this one should too. but it should support all media assets, not just the podcasts. it should include blogs, and more.

## episode segments

really, we should be able to support anybody's particular sequence of audio files.
then we can get managed files associated with a given podcast and make them avbailalbe from a drop down when configuring
a new episode. as youre configuring the episode, u can also just elect to in-situ upload new segments, which have their
own ordering.
so, podcasts will have 'commonly used managed files', and youll be able to associate 1-N managed files to each podcast
episode.
right now, its super ineffeciently doing all the sorting on the server and doing a client side reload of all the
segments. which means we'll need an event that goes off to allow us to know when to disable the UI. for example, you
shouldnt be able to upload a file when youve clicked the up or down arrow, because that data will change. likewise, you
shouldnt be able to press the up, delete, or down buttons wheen youre in the middle of a file upload. so we need events
on the segment too.

youre working on getting segments to flow through the python code as an unlimited list of segments

i should render the segment UI as sort of being like the grahic managed file upload, but that’s a required segment,
whereas everything after (beyond the first audio segment) is not mandatory. come to think of it tho, as soon as u create
a episode draft, we should automatically add ur first episode segment, too. since theres no such thing as a valid
episode without a single segment

make the up/down icons nicer

add a New Segment button so you can add them

figure out how to communicate validity or invalidty when the number of segments gets to 1 element

## managed file settings
we need a way for a user to specify which intro, bumper, and outro to  use for each podbean publication. but they're going to have to uplaod a value. right now the settings page only supports text values. how do we support uploading files. what do we put in the storage#write method? the S3 URL of the ManagedFile?

## settings page: hide the configuration values on load. don't want somebody over-the-shouldering your company secrets or whatever. 
Find a widget to hide the contents of textboxes  (show/hide eye icon or something) so that the values in the settings page are by default hidden


## publications

show all the publications underneath the publish button. it should show the contents of the publication table, and in particular: the date, the plugin used, and - if possible - a button that allows the user to delete the publication. or, at least try. maybe its greyed out if the user has already deleted that publication or if the plugin indicates it can’t delete a givne publication (mybe the tech doesn’t allow it?) who knows. ALSO; should there a publication_URL field in the DB that the user can click to visit it? like, right now i still dont feeel 100% comfortable having the plugin make the Podbean podcast _live_. i want it to be draft so i can do one more quality check there, sort of a staging thing. so i want to knmow which url to visit to edit, preview, whatever, the final thing on podbean before i push the button to go 'live'


