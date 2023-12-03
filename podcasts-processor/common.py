NAME = "Podcast Processor"


def normalize_string(s: str):
    import string

    return "".join([c for c in s if (c in string.digits or c in string.ascii_letters)])
