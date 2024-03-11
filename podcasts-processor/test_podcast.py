import os.path
import subprocess
import typing
from unittest import TestCase

import podcast


def generate_sample_audio_files(say_output_file_dirname: str, parts: typing.List[str]):
    results = []
    ctr = 1
    for to_say in parts:
        say_output_file_name = f'{ctr}.aiff'
        say_output_file = os.path.join(say_output_file_dirname, say_output_file_name)
        if not os.path.exists(say_output_file_dirname):
            os.makedirs(say_output_file_dirname)
        assert os.path.isdir(say_output_file_dirname), f'the directory in which you want to write {say_output_file_name} does not exist'
        print(say_output_file)
        cmd = ['say', '-o', f'{say_output_file}', f""" "{to_say}" """.strip()]
        print(cmd)
        completed_process = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        assert completed_process.returncode == 0, f'the process didnt finish normally'
        print(completed_process.returncode, completed_process.stdout)
        results.append(say_output_file)
        ctr += 1
    return results


class TestCreatingPodcastAudio(TestCase):

    def test_creating_podcast_audio(self):
        current_dir = os.path.dirname(os.path.realpath(__file__))
        numbers = 'first,second,third,fourth,fifth,sixth'.split(',')
        to_say = [f'this is the {p} part in this podcast' for p in numbers]
        sample_audio_folder = os.path.join(current_dir, 'test-parts')
        samples: typing.List[str] = []
        if not os.path.exists(sample_audio_folder) or len(os.listdir(sample_audio_folder)) < len(numbers):
            print('creating sample audio files')
            samples = generate_sample_audio_files(sample_audio_folder, to_say)
        else:
            samples = [os.path.join(sample_audio_folder, sample) for sample in os.listdir(sample_audio_folder) if
                       sample.endswith('.aiff')]
            # now we have the files, let's turn 'em into a podcast

        samples.sort()
        segments = [podcast.Segment(audio_file=sample, ext='aiff') for sample in samples]
        output_podcast = podcast.create_podcast(segments, os.path.join(sample_audio_folder, 'output.wav'), 'wav')
        assert os.path.exists(output_podcast), f'the podcast file {output_podcast} was not successfully generated'


if __name__ == "__main__":
    import unittest

    unittest.main()
