detector = vision.CascadeObjectDetector('../data/detector.xml');
disp('Detector');
img = imread('../data/raw/w_501.jpg');
disp('imread');
bbox = step(detector, img);
disp('bbox');
detectedImg = insertObjectAnnotation(img,'rectangle',bbox,'whale');
disp('detectedImg');
disp(bbox(2, :))

%detectedImg = imcrop(img, bbox(8,:));

figure;
disp('Figure');
imshow(detectedImg);
disp('Done.');