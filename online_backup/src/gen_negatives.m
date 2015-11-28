images = imageSet('../data/lab/');
data = load('../data/lab/labelingSession.mat');

formatStr = '../data/neg/neg%d.jpg';   % Output format for negatives
fprintf('%s\n', 'Generating negatives.')
parfor i=1:images.Count
   fprintf('%s%i\n', 'Cropping image: ', i)
   imginp = read(images,i);  % Read an image 
   imcropped = imcrop(imginp,[1 1 1078 670]); % Crop
   fileName = sprintf(formatStr,i);
   imwrite(imcropped,fileName); % Save negative images
end

fprintf('%s\n', 'Done cropping images.')