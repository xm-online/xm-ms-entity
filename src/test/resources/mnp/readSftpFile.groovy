if (lepContext.inArgs.args[1] == 'portedListDAILY') {
return [content: """<?xml version="1.0" encoding="UTF-8" ?>
<items>
    <ported>
        <number>380959043208</number>
        <portedDate>2025-02-13T10:03:21.960Z</portedDate>
        <recipientRC>3906</recipientRC>
        <donorRC>3901</donorRC>
        <portedAction>INSERT</portedAction>
    </ported>
    <ported>
        <number>380950666666</number>
        <portedDate>2025-02-13T10:03:21.960Z</portedDate>
        <recipientRC>3906</recipientRC>
        <donorRC>3901</donorRC>
        <portedAction>INSERT</portedAction>
    </ported>
</items>
""".toString()]
} else if (lepContext.inArgs.args[1] == 'returnListDAILY') {
return [content: """<?xml version="1.0" encoding="UTF-8" ?>
<items></items>
""".toString()]
}
throw new Exception("Unknown file type")
