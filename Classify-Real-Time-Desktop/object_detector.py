import os
import numpy as np
from six import BytesIO
from PIL import Image, ImageDraw, ImageFont

import tensorflow as tf

from object_detection.utils import label_map_util
from object_detection.utils import config_util
from object_detection.utils import visualization_utils as viz_utils
from object_detection.builders import model_builder

PATH_TO_TF_API = "../../models/research/"
MODELS = {'centernet_with_keypoints': 'centernet_mobilenetv2_fpn_kpts', 'efficient_net': 'efficientdet_d5_coco17_tpu-32'}
MODEL_URLS = {'efficient_net': 'http://download.tensorflow.org/models/object_detection/tf2/20200711/efficientdet_d5_coco17_tpu-32.tar.gz',
              'centernet_with_keypoints': 'http://download.tensorflow.org/models/object_detection/tf2/20210210/centernet_mobilenetv2fpn_512x512_coco17_kpts.tar.gz'}
MODEL_DISPLAY_NAME = 'centernet_with_keypoints' # @param ['centernet_with_keypoints', 'centernet_without_keypoints']
MODEL_NAME = MODELS[MODEL_DISPLAY_NAME]
MODEL_URL = MODEL_URLS[MODEL_DISPLAY_NAME]


def load_image_into_numpy_array(path):
    """Load an image from file into a numpy array.

    Puts image into numpy array to feed into tensorflow graph.
    Note that by convention we put it into a numpy array with shape
    (height, width, channels), where channels=3 for RGB.

    Args:
      path: a file path (this can be local or on colossus)

    Returns:
      uint8 numpy array with shape (img_height, img_width, 3)
    """
    img_data = tf.io.gfile.GFile(path, 'rb').read()
    image = Image.open(BytesIO(img_data))
    (im_width, im_height) = image.size
    return np.array(image.getdata()).reshape(
      (im_height, im_width, 3)).astype(np.uint8)

def get_model_detection_function():

    config_path = os.path.join(PATH_TO_TF_API, 'object_detection/test_data/', MODEL_NAME, 'pipeline.config')
    configs = config_util.get_configs_from_pipeline_file(config_path)
    model_config = configs['model']
    label_map_path =  os.path.join(PATH_TO_TF_API, 'object_detection/test_data/', MODEL_NAME, 'label_map.txt')
    model_config.center_net.keypoint_label_map_path = label_map_path
    detection_model = model_builder.build(
      model_config=model_config, is_training=False)

    ckpt = tf.compat.v2.train.Checkpoint(model=detection_model)
    ckpt.restore(os.path.join(PATH_TO_TF_API, 'object_detection/test_data/', MODEL_NAME, 'checkpoint', 'ckpt-301'))

    @tf.function
    def detect_fn(image):
        """Detect objects in image."""

        image, shapes = detection_model.preprocess(image)
        prediction_dict = detection_model.predict(image, shapes)
        detections = detection_model.postprocess(prediction_dict, shapes)

        return detections, prediction_dict, tf.reshape(shapes, [-1])
    label_map = label_map_util.load_labelmap(label_map_path)
    categories = label_map_util.convert_label_map_to_categories(
                    label_map,
                    max_num_classes=label_map_util.get_max_label_map_index(label_map),
                    use_display_name=True)
    category_index = label_map_util.create_category_index(categories)
    label_map_dict = label_map_util.get_label_map_dict(label_map, use_display_name=True)

    return detect_fn, label_map_dict, category_index

