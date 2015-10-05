images = imageSet('../data/lab/');
data = load('../data/lab/labelingSession.mat');

formatStr = '../data/neg/neg%d.jpg';   % Output format for negatives
for i=1:images.Count 
    imginp = read(images,i);  % Read an image 
    imcropped = imcrop(imginp,[1 1 1078 670]); % Crop
    fileName = sprintf(formatStr,i);
    imwrite(imcropped,fileName); % Save negative images
end

oldfolder = cd ('../data/');
trainCascadeObjectDetector('detector.xml', ...
                     positiveInstances, 'neg/', ...
                     'NumCascadeStages',15,'FalseAlarmRate',0.01, ...
                     'FeatureType','LBP');
                 
cd (oldfolder);