#!/usr/bin/env python3

from pydub import AudioSegment

from utils import *

logger = logging.getLogger("pydub.converter")
logger.setLevel(logging.DEBUG)
logger.addHandler(logging.StreamHandler())


class Segment(object):
    def __init__(self, audio_file: str, ext: str, crossfade_time: int = 100):
        self.audio_file = audio_file
        self.extension = ext
        self.crossfade_time = crossfade_time


def create_podcast(audio_files: typing.List[Segment], output_file_path: str, output_extension='mp3') -> str:
    """ creates podcast a then returns the file path of the resulting audio file """
    assert len(audio_files) > 0, 'there should be at least one segment for this clip'
    assert audio_files is not None, 'there should be at least one segment for this'
    assert output_file_path is not None, 'you should provide an output file path'
    path_dirname = os.path.dirname(output_file_path)
    assert os.path.exists(path_dirname) or os.makedirs(
        path_dirname), f'the directory to which you want to write, {path_dirname}, does not exist.'
    audio_segments = [(AudioSegment.from_file(a.audio_file, format=a.extension), a.crossfade_time) for a in audio_files]
    out = audio_segments[0][0]
    for segment, crossfade_time in audio_segments[1:]:
        out = out.append(segment, crossfade=crossfade_time)
    out.export(output_file_path, format=output_extension, bitrate="256k")
    return output_file_path
