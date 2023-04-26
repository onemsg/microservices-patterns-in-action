import json
import pathlib
import logging

LOGGER_FORMAT = '%(asctime)s %(levelname)s - %(message)s'

logging.basicConfig(format=LOGGER_FORMAT, level='INFO')

def __read_config(config_file):
    _ = pathlib.Path(__file__).parent.joinpath(config_file)
    with open(_, "r", encoding="UTF-8") as f:
        return json.load(f)

__CONFIG_FILE = "config.json"

config = __read_config(__CONFIG_FILE)

if __name__ == "__main__":
    print("INIT")